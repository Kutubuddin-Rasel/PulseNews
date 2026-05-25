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
import kotlinx.coroutines.flow.map
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
    private val algoPrefsRepo: AlgorithmPreferencesRepository,
    private val feedScorer: FeedScorer,
    private val taxonomyRepository: com.example.newsapp.data.repository.TaxonomyRepository
) : NewsRepository {

    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun getFeed(categoryId: Int, keyword: String?, source: String?): Flow<androidx.paging.PagingData<Article>> {
        val feedKey = if (categoryId == 1) "for_you" else "firehose"

        // If a keyword is provided, intercept the flow and use the backend Search API directly!
        if (!keyword.isNullOrBlank()) {
            return androidx.paging.Pager(
                config = androidx.paging.PagingConfig(
                    pageSize = 20,
                    prefetchDistance = 5,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { 
                    SearchPagingSource(pulseBackendApi, connectivityMonitor, keyword) 
                }
            ).flow
        }

        // If it's category 1 (For You) and no filters are applied, use the RemoteMediator to fetch from backend.
        // Otherwise, only use local data.
        val useRemoteMediator = categoryId == 1 && source.isNullOrBlank()

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
                    val (matchQuery, hasMatch) = buildFtsMatchQuery(categoryId, keyword)
                    val hasSource = if (source.isNullOrBlank()) 0 else 1
                    
                    if (hasMatch == 1 && hasSource == 1) {
                        database.cachedFeedDao().getFilteredFeedWithMatchAndSource(matchQuery, source!!)
                    } else if (hasMatch == 1) {
                        database.cachedFeedDao().getFilteredFeedWithMatch(matchQuery)
                    } else if (hasSource == 1) {
                        database.cachedFeedDao().getFilteredFeedWithSource(source!!)
                    } else {
                        database.cachedFeedDao().getFilteredFeedAll()
                    }
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainArticle() }
        }
    }

    override fun getAvailableSources(): Flow<List<String>> {
        return database.cachedFeedDao().getAvailableSources()
    }

    private fun buildFtsMatchQuery(categoryId: Int, keyword: String?): Pair<String, Int> {
        val matchTerms = mutableListOf<String>()

        // 1. Dynamic Category logic via Hybrid ML Taxonomy (No more hardcoded rules!)
        val categoryKey = when (categoryId) {
            2 -> "tech"
            3 -> "business"
            4 -> "politics"
            5 -> "sports"
            6 -> "entertainment"
            7 -> "health"
            else -> null
        }

        if (categoryKey != null) {
            // Because this executes in a PagingSource block synchronously, we need the latest value.
            // Using runBlocking inside a Repository is usually an anti-pattern, but PagingSource
            // load() runs on a background thread so it's safe here, or we can use firstOrNull().
            val dictionary = kotlinx.coroutines.runBlocking { taxonomyRepository.dictionaryFlow.first() }
            val keywords = dictionary[categoryKey]
            if (!keywords.isNullOrEmpty()) {
                // Wrap each keyword in double quotes to support multi-word phrases (e.g. "Apple Vision Pro") in FTS
                val categoryMatch = "(" + keywords.joinToString(" OR ") { "\"$it\"" } + ")"
                matchTerms.add(categoryMatch)
            }
        }

        // 2. Keyword logic
        if (!keyword.isNullOrBlank()) {
            val sanitizedKeyword = keyword.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
            if (sanitizedKeyword.isNotEmpty()) {
                val words = sanitizedKeyword.split("\\s+".toRegex())
                val prefixQuery = words.joinToString(" ") { "$it*" }
                matchTerms.add("($prefixQuery)")
            }
        }

        return if (matchTerms.isNotEmpty()) {
            Pair(matchTerms.joinToString(" AND "), 1)
        } else {
            Pair("", 0)
        }
    }

    override suspend fun cachedArticleByUrl(url: String): Article? {
        return database.cachedFeedDao().findOneByUrl(url)?.toDomainArticle()
    }

    override suspend fun syncFirehose(): Result<Unit> {
        if (!connectivityMonitor.isOnline()) {
            return Result.failure(IOException("Offline"))
        }

        return try {
            val response = pulseBackendApi.getNewsFeed(cursor = null)
            if (response.isSuccessful) {
                val articlesDto = response.body() ?: emptyList()
                val articles = articlesDto.mapNotNull { it.toDomainOrNull() }
                val fetchedAt = clockProvider.nowMillis()

                database.withTransaction {
                    // Clear the old firehose cache and remote keys to ensure a clean state
                    database.cachedFeedDao().clearFeed("firehose")

                    val currentPrefs = algoPrefsRepo.preferences.first()

                    val entities = articles.mapIndexed { index, article ->
                        val score = feedScorer.computeScore(article, currentPrefs, fetchedAt)
                        article.toCacheEntity(
                            feedKey = "firehose",
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

    override suspend fun getTrendingTopics(): Result<List<String>> {
        if (!connectivityMonitor.isOnline()) {
            return Result.failure(IOException("Offline"))
        }
        return try {
            val response = pulseBackendApi.getTrendingTopics()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(retrofit2.HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Refactored method to recalculate scores using chunked keyset pagination (O(1) memory)
    suspend fun recalculateAllScores() {
        val currentPrefs = algoPrefsRepo.preferences.first()
        val currentTime = clockProvider.nowMillis()
        
        var lastProcessedId = 0
        val chunkSize = 500

        while (true) {
            val chunk = database.cachedFeedDao().getChunkByKeyset(lastProcessedId, chunkSize)
            if (chunk.isEmpty()) break

            val updatedEntities = mutableListOf<com.example.newsapp.Room.CachedFeedArticleEntity>()

            chunk.forEach { entity ->
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
                val newScore = feedScorer.computeScore(article, currentPrefs, currentTime)
                if (newScore != entity.relevanceScore) {
                    updatedEntities.add(entity.copy(relevanceScore = newScore))
                }
                
                // Track the highest ID in this chunk for the next query
                if (entity.id > lastProcessedId) {
                    lastProcessedId = entity.id
                }
            }

            if (updatedEntities.isNotEmpty()) {
                // Execute a single bulk array binding instead of N individual queries
                database.withTransaction {
                    database.cachedFeedDao().updateAll(updatedEntities)
                }
            }
        }
    }
}
