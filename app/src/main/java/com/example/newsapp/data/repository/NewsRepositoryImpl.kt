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
import androidx.paging.map
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import com.example.newsapp.domain.util.FeedScorer
import kotlinx.coroutines.flow.first

class NewsRepositoryImpl @Inject constructor(
    private val pulseBackendApi: PulseBackendApi,
    private val database: ArticleDatabase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val clockProvider: ClockProvider,
    private val telemetry: AppTelemetry,
    private val algoPrefsRepo: AlgorithmPreferencesRepository
) : NewsRepository {

    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun getFeed(categoryId: Int, keyword: String?, source: String?): Flow<androidx.paging.PagingData<Article>> {
        val feedKey = "firehose"

        // If it's category 1 (For You) and no filters are applied, use the RemoteMediator to fetch from backend.
        // Otherwise, only use local data.
        val useRemoteMediator = categoryId == 1 && keyword.isNullOrBlank() && source.isNullOrBlank()

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
                    telemetry = telemetry
                )
            } else null,
            pagingSourceFactory = {
                if (useRemoteMediator) {
                    database.cachedFeedDao().getByFeedKey(feedKey)
                } else {
                    val query = buildLocalQuery(categoryId, keyword, source)
                    database.cachedFeedDao().getFilteredFeed(query)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainArticle() }
        }
    }

    override fun getAvailableSources(): Flow<List<String>> {
        return database.cachedFeedDao().getAvailableSources()
    }

    private fun buildLocalQuery(categoryId: Int, keyword: String?, source: String?): androidx.sqlite.db.SupportSQLiteQuery {
        val hasCategory = categoryId in 2..7
        val hasKeyword = !keyword.isNullOrBlank()
        
        val baseQuery = if (hasCategory || hasKeyword) {
            "SELECT cached_feed_articles.* FROM cached_feed_articles JOIN cached_feed_fts ON cached_feed_articles.id = cached_feed_fts.rowid WHERE 1=1"
        } else {
            "SELECT * FROM cached_feed_articles WHERE 1=1"
        }
        
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any>()
        val matchTerms = mutableListOf<String>()

        // 1. Category logic (FTS MATCH)
        when (categoryId) {
            2 -> matchTerms.add("(tech OR AI OR software OR Apple OR Google)")
            3 -> matchTerms.add("(economy OR stock OR market OR crypto OR finance OR business)")
            4 -> matchTerms.add("(politics OR government OR election OR world OR congress)")
            5 -> matchTerms.add("(sports OR football OR basketball OR soccer OR NBA OR NFL)")
            6 -> matchTerms.add("(movie OR music OR hollywood OR celebrity OR entertainment)")
            7 -> matchTerms.add("(health OR science OR space OR medicine OR NASA)")
        }

        // 2. Keyword logic (FTS MATCH)
        if (hasKeyword) {
            val sanitizedKeyword = keyword!!.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
            if (sanitizedKeyword.isNotEmpty()) {
                val words = sanitizedKeyword.split("\\s+".toRegex())
                val prefixQuery = words.joinToString(" ") { "$it*" }
                matchTerms.add("($prefixQuery)")
            }
        }

        if (matchTerms.isNotEmpty()) {
            val matchQuery = matchTerms.joinToString(" ")
            clauses.add("cached_feed_fts MATCH ?")
            args.add(matchQuery)
        }
        
        // 3. Source logic (Standard WHERE)
        if (!source.isNullOrBlank()) {
            clauses.add("cached_feed_articles.sourceName = ?")
            args.add(source)
        }

        val finalQueryString = if (clauses.isEmpty()) {
            "$baseQuery ORDER BY cached_feed_articles.page ASC, cached_feed_articles.sortOrder ASC"
        } else {
            "$baseQuery AND " + clauses.joinToString(" AND ") + " ORDER BY cached_feed_articles.page ASC, cached_feed_articles.sortOrder ASC"
        }

        return androidx.sqlite.db.SimpleSQLiteQuery(finalQueryString, args.toTypedArray())
    }

    override suspend fun cachedArticleByUrl(url: String): Article? {
        return database.cachedFeedDao().findOneByUrl(url)?.toDomainArticle()
    }

    override suspend fun syncFirehose(): Result<Unit> {
        if (!connectivityMonitor.isOnline()) {
            return Result.failure(IOException("Offline"))
        }

        return try {
            val response = pulseBackendApi.getNewsFeed(page = 1)
            if (response.isSuccessful) {
                val articlesDto = response.body() ?: emptyList()
                val articles = articlesDto.mapNotNull { it.toDomainOrNull() }
                val fetchedAt = clockProvider.nowMillis()

                database.withTransaction {
                    // Clear the old firehose cache and remote keys to ensure a clean state
                    database.remoteKeysDao().clearRemoteKeys()
                    database.cachedFeedDao().clearFeed("firehose")

                    // Insert the fresh page 1 data
                    val nextKey = if (articles.isEmpty()) null else 2
                    val keys = articles.map {
                        com.example.newsapp.Room.RemoteKeys(url = it.url, prevKey = null, nextKey = nextKey)
                    }
                    database.remoteKeysDao().insertAll(keys)

                    val currentPrefs = algoPrefsRepo.preferences.first()

                    val entities = articles.mapIndexed { index, article ->
                        val score = FeedScorer.computeScore(article, currentPrefs, fetchedAt)
                        article.toCacheEntity(
                            feedKey = "firehose",
                            page = 1,
                            sortOrder = index,
                            fetchedAt = fetchedAt
                        ).copy(relevanceScore = score)
                    }
                    database.cachedFeedDao().upsertAll(entities)
                }
                Result.success(Unit)
            } else {
                Result.failure(retrofit2.HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNewsMetaLastUpdated(): Result<String?> {
        if (!connectivityMonitor.isOnline()) {
            return Result.failure(IOException("Offline"))
        }
        return try {
            val response = pulseBackendApi.getNewsMeta()
            if (response.isSuccessful) {
                Result.success(response.body()?.lastUpdated)
            } else {
                Result.failure(retrofit2.HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // New method to recalculate scores when preferences change
    suspend fun recalculateAllScores() {
        val currentPrefs = algoPrefsRepo.preferences.first()
        val currentTime = clockProvider.nowMillis()
        
        database.withTransaction {
            val allEntities = database.cachedFeedDao().getAll()
            allEntities.forEach { entity ->
                // Map back to domain article temporarily to use the generic FeedScorer
                val article = Article(
                    author = entity.author,
                    content = entity.content,
                    description = entity.description,
                    publishedAt = entity.publishedAt,
                    source = com.example.newsapp.module.Source(
                        id = entity.sourceId,
                        name = entity.sourceName
                    ),
                    title = entity.title,
                    url = entity.url,
                    urlToImage = entity.urlToImage
                )
                val newScore = FeedScorer.computeScore(article, currentPrefs, currentTime)
                if (newScore != entity.relevanceScore) {
                    database.cachedFeedDao().updateRelevanceScore(entity.id, newScore)
                }
            }
        }
    }
}
