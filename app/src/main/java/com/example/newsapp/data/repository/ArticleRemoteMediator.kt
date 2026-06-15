package com.example.newsapp.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.Room.ArticleDatabase
import com.example.newsapp.Room.CachedFeedArticleEntity
import com.example.newsapp.data.mapper.toCacheEntity
import com.example.newsapp.data.mapper.toDomainOrNull
import com.example.newsapp.domain.util.AppTelemetry
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.domain.repository.AlgorithmPreferencesRepository
import com.example.newsapp.domain.repository.FeedMetaRepository
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediator(
    private val feedKey: String,
    private val pulseBackendApi: PulseBackendApi,
    private val database: ArticleDatabase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val clockProvider: ClockProvider,
    private val telemetry: AppTelemetry,
    private val algorithmPreferencesRepository: AlgorithmPreferencesRepository,
    private val feedMetaRepository: FeedMetaRepository
) : RemoteMediator<Int, CachedFeedArticleEntity>() {

    private val cacheFreshnessMs = 15 * 60 * 1000L

    // O7: meta (totalPages/lastUpdated) changes far more slowly than the feed, so give it its own,
    // longer TTL. The feed still refreshes every cacheFreshnessMs, but the meta endpoint is hit at
    // most once an hour instead of on every refresh.
    private val metaFreshnessMs = 60 * 60 * 1000L

    override suspend fun initialize(): InitializeAction {
        val latestFetchTime = database.cachedFeedDao().latestFetchTime(feedKey)
        val cacheAgeMs = latestFetchTime?.let { clockProvider.nowMillis() - it }
        
        return if (cacheAgeMs != null && cacheAgeMs <= cacheFreshnessMs) {
            telemetry.info("RemoteMediator", "Cache is fresh for $feedKey. Age: $cacheAgeMs ms")
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CachedFeedArticleEntity>
    ): MediatorResult {
        if (!connectivityMonitor.isOnline()) {
            telemetry.warn("RemoteMediator", "Offline, returning cached data for $feedKey")
            return MediatorResult.Error(IOException("You're offline and no cached content is available yet."))
        }

        return try {
            val page: Int = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                    val nextPage = (lastItem.sortOrder / state.config.pageSize) + 2
                    
                    // Check global totalPages if available
                    val meta = feedMetaRepository.meta.first()
                    if (meta != null && nextPage > meta.totalPages) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    nextPage
                }
            }

            // Fetch meta on refresh, but only when the cached meta has aged past its (longer) TTL.
            // O7: this skips a redundant network round-trip on every feed refresh — the meta is
            // re-fetched at most hourly even though the feed itself refreshes every 15 minutes.
            if (loadType == LoadType.REFRESH) {
                val metaFetchedAt = feedMetaRepository.metaFetchedAtMs.first()
                val metaAgeMs = metaFetchedAt?.let { clockProvider.nowMillis() - it }
                val metaIsFresh = metaAgeMs != null && metaAgeMs <= metaFreshnessMs
                if (!metaIsFresh) {
                    try {
                        val metaResponse = pulseBackendApi.getNewsMeta()
                        if (metaResponse.isSuccessful) {
                            metaResponse.body()?.let { meta ->
                                feedMetaRepository.saveMeta(meta.totalPages, meta.lastUpdated, clockProvider.nowMillis())
                            }
                        }
                    } catch (e: Exception) {
                        telemetry.warn("RemoteMediator", "Failed to fetch meta: ${e.message}")
                    }
                }
            }

            val response = if (feedKey == "for_you" || feedKey == "firehose") {
                // If it's firehose, it means all categories, so category = null
                if (feedKey == "for_you") {
                    val weights = algorithmPreferencesRepository.preferences.first()
                    val isDefault = weights.isNotEmpty() && weights.values.all { it == 0.2f }
                    
                    if (isDefault) {
                        telemetry.info("RemoteMediator", "Unlocking Tier 2 Vector Semantic Matching (weights=null)")
                    }
                    
                    val weightsStr = if (isDefault) null else "technology:${weights["technology"]},politics:${weights["politics"]},general:${weights["general"]},business:${weights["business"]},health:${weights["health"]}"
                    pulseBackendApi.getForYouFeed(page = page, limit = state.config.pageSize, weights = weightsStr)
                } else {
                    pulseBackendApi.getNewsFeed(page = page, limit = state.config.pageSize, category = null)
                }
            } else {
                // feedKey is a specific category like "tech", "business", etc.
                pulseBackendApi.getNewsFeed(page = page, limit = state.config.pageSize, category = feedKey)
            }

            if (response.isSuccessful) {
                val articlesDto = response.body() ?: emptyList()
                val articles = articlesDto.mapNotNull { it.toDomainOrNull() }
                // CONF5: surface silent drops (DTOs rejected for missing link/title) instead of
                // swallowing them — a sudden spike signals an upstream contract change.
                val dropped = articlesDto.size - articles.size
                if (dropped > 0) {
                    android.util.Log.w(
                        "ArticleMapper",
                        "Dropped $dropped/${articlesDto.size} feed articles (feedKey=$feedKey) — missing link/title"
                    )
                }
                val endOfPaginationReached = articles.size < state.config.pageSize
                val fetchedAt = clockProvider.nowMillis()

                val entities = articles.mapIndexed { index, article ->
                    val offset = (page - 1) * state.config.pageSize
                    article.toCacheEntity(
                        feedKey = feedKey,
                        sortOrder = offset + index,
                        fetchedAt = fetchedAt
                    )
                }

                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        database.cachedFeedDao().clearFeed(feedKey)
                    }
                    database.cachedFeedDao().upsertAll(entities)
                }

                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            } else {
                MediatorResult.Error(HttpException(response))
            }
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
