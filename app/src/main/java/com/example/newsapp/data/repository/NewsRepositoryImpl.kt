package com.example.newsapp.data.repository

import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.BuildConfig
import com.example.newsapp.Room.ArticleDatabase
import androidx.room.withTransaction
import com.example.newsapp.data.mapper.toCacheEntity
import com.example.newsapp.data.mapper.toDomainArticle
import com.example.newsapp.data.mapper.toDomainOrNull
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.domain.model.AppError
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val pulseBackendApi: PulseBackendApi,
    private val database: ArticleDatabase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val clockProvider: ClockProvider,
    private val telemetry: AppTelemetry
) : NewsRepository {

    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun topHeadlines(category: String): Flow<androidx.paging.PagingData<Article>> {
        val feedKey = "firehose"
        
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = ArticleRemoteMediator(
                feedKey = feedKey,
                pulseBackendApi = pulseBackendApi,
                database = database,
                connectivityMonitor = connectivityMonitor,
                clockProvider = clockProvider,
                telemetry = telemetry
            ),
            pagingSourceFactory = { database.cachedFeedDao().getByFeedKey(feedKey) }
        ).flow.kotlinx.coroutines.flow.map { pagingData ->
            pagingData.map { it.toDomainArticle() }
        }
    }

    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun everything(query: EverythingQuery): Flow<androidx.paging.PagingData<Article>> {
        // Fallback to the same firehose since backend has no filtering
        return topHeadlines("")
    }

    override suspend fun cachedArticleByUrl(url: String): Article? {
        return database.cachedFeedDao().findOneByUrl(url)?.toDomainArticle()
    }
}
