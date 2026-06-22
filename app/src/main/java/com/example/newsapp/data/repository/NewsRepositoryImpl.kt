package com.example.newsapp.data.repository

import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.BuildConfig
import com.example.newsapp.Room.ArticleDatabase
import androidx.room.withTransaction
import com.example.newsapp.data.mapper.toCacheEntity
import com.example.newsapp.data.mapper.toDomainArticle
import com.example.newsapp.data.mapper.toDomainOrNull
import com.example.newsapp.data.mapper.toDomain
import com.example.newsapp.domain.util.AppTelemetry
import com.example.newsapp.domain.model.AppError
import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.TrendingTopic
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.domain.repository.AlgorithmPreferencesRepository
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import androidx.paging.map
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.newsapp.domain.repository.FeedMetaRepository

class NewsRepositoryImpl @Inject constructor(
    private val pulseBackendApi: PulseBackendApi,
    private val database: ArticleDatabase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val clockProvider: ClockProvider,
    private val telemetry: AppTelemetry,
    private val algorithmPreferencesRepository: AlgorithmPreferencesRepository,
    private val feedMetaRepository: FeedMetaRepository,
    private val searchResultCache: SearchResultCache
) : NewsRepository {

    // Single source of truth for the categoryKey → cached-feed key mapping (used by the feed query
    // and the F1 source-list scoping below).
    private fun feedKeyFor(categoryKey: CategoryKey): String =
        if (categoryKey == CategoryKey.FOR_YOU) "for_you" else categoryKey.value

    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun getFeed(categoryKey: CategoryKey, source: String?): Flow<androidx.paging.PagingData<Article>> {
        val feedKey = feedKeyFor(categoryKey)

        // Use RemoteMediator to handle graceful backend pagination and local cache appending for all feeds
        val useRemoteMediator = source.isNullOrBlank()

        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = if (useRemoteMediator) {
                ArticleRemoteMediator(
                    feedKey = feedKey,
                    pulseBackendApi = pulseBackendApi,
                    database = database,
                    connectivityMonitor = connectivityMonitor,
                    clockProvider = clockProvider,
                    telemetry = telemetry,
                    algorithmPreferencesRepository = algorithmPreferencesRepository,
                    feedMetaRepository = feedMetaRepository
                )
            } else null,
            pagingSourceFactory = {
                if (source.isNullOrBlank()) {
                    database.cachedFeedDao().getByFeedKey(feedKey)
                } else {
                    database.cachedFeedDao().getFilteredFeedByCategory(feedKey, source)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainArticle() }
        }
    }

    override fun searchNews(query: String, excludeId: String?): Flow<androidx.paging.PagingData<Article>> {
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SearchPagingSource(pulseBackendApi, connectivityMonitor, searchResultCache, query, excludeId)
            }
        ).flow
    }

    override fun getAvailableSources(categoryKey: CategoryKey): kotlinx.coroutines.flow.Flow<List<String>> {
        return database.cachedFeedDao().getAvailableSources(feedKeyFor(categoryKey))
    }

    override suspend fun cachedArticleByUrl(url: String): Article? = withContext(Dispatchers.IO) {
        return@withContext database.cachedFeedDao().findOneByUrl(url)?.toDomainArticle()
    }


    override fun getTrendingTopics(): Flow<List<TrendingTopic>> {
        return database.trendingTopicDao().getTrendingTopics().map { entities ->
            entities.map { TrendingTopic(tag = it.tag, count = it.count) }
        }
    }
}
