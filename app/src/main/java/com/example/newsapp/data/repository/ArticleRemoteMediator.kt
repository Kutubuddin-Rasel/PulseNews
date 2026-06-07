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
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.domain.repository.AlgorithmPreferencesRepository
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
    private val algorithmPreferencesRepository: AlgorithmPreferencesRepository
) : RemoteMediator<Int, CachedFeedArticleEntity>() {

    private val cacheFreshnessMs = 15 * 60 * 1000L

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
                    // For RemoteMediator, calculating the next page based on local offset is reliable
                    // because we clear the DB on refresh and append linearly.
                    // Instead of full count, dividing the current pageSize by max items is sufficient,
                    // but we can just use a simple calculation based on state's pages.
                    // Wait, `state.pages.size` is often the number of pages loaded in memory.
                    // Since backend pagination starts at page 1, and we prepend 1 on append,
                    // Let's use `(database.cachedFeedDao().countFeedItems(feedKey) / state.config.pageSize) + 1`
                    // But actually, we don't have countFeedItems. Let's just track it or use highest sortOrder.
                    // Let's check `state.lastItemOrNull()`.
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                    (lastItem.sortOrder / state.config.pageSize) + 2
                }
            }

            val response = if (feedKey == "for_you" || feedKey == "firehose") {
                // If it's firehose, it means all categories, so category = null
                if (feedKey == "for_you") {
                    val weights = algorithmPreferencesRepository.preferences.first()
                    val weightsStr = "technology:${weights["technology"]},politics:${weights["politics"]},general:${weights["general"]},business:${weights["business"]},health:${weights["health"]}"
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
                val endOfPaginationReached = articles.isEmpty()
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
