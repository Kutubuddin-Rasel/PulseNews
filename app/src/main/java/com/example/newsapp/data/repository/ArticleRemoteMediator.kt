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
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediator(
    private val feedKey: String,
    private val pulseBackendApi: PulseBackendApi,
    private val database: ArticleDatabase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val clockProvider: ClockProvider,
    private val telemetry: AppTelemetry
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
            val cursor: String? = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    state.lastItemOrNull()?.backendId
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                }
            }

            val response = if (feedKey == "for_you") {
                pulseBackendApi.getForYouFeed(cursor = cursor, limit = state.config.pageSize)
            } else {
                pulseBackendApi.getNewsFeed(cursor = cursor, limit = state.config.pageSize)
            }

            if (response.isSuccessful) {
                val articlesDto = response.body() ?: emptyList()
                val articles = articlesDto.mapNotNull { it.toDomainOrNull() }
                val endOfPaginationReached = articles.isEmpty()
                val fetchedAt = clockProvider.nowMillis()

                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        database.cachedFeedDao().clearFeed(feedKey)
                    }

                    val entities = articles.mapIndexed { index, article ->
                        article.toCacheEntity(
                            feedKey = feedKey,
                            sortOrder = index,
                            fetchedAt = fetchedAt
                        )
                    }
                    database.cachedFeedDao().upsertAll(entities)
                }

                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            } else {
                MediatorResult.Error(HttpException(response))
            }
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}
