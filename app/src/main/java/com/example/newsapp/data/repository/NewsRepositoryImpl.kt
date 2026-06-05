package com.example.newsapp.data.repository

import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.BuildConfig
import com.example.newsapp.Room.ArticleDatabase
import androidx.room.withTransaction
import com.example.newsapp.data.mapper.toCacheEntity
import com.example.newsapp.data.mapper.toDomainArticle
import com.example.newsapp.data.mapper.toDomainOrNull
import com.example.newsapp.data.mapper.toDomain
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.domain.model.AppError
import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.TrendingTopic
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    override fun getFeed(categoryKey: CategoryKey, keyword: String?, source: String?): Flow<androidx.paging.PagingData<Article>> {
        val feedKey = if (categoryKey == CategoryKey.FOR_YOU) "for_you" else "firehose"

        // If a keyword is provided and device is online, intercept the flow and use the backend Search API directly!
        if (!keyword.isNullOrBlank() && connectivityMonitor.isOnline()) {
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

        // Use RemoteMediator to handle graceful backend pagination and local cache appending for all feeds
        val useRemoteMediator = source.isNullOrBlank()

        return taxonomyRepository.dictionaryFlow
            .map { it[categoryKey] ?: emptyList() }
            .distinctUntilChanged()
            .flatMapLatest { keywords ->
                androidx.paging.Pager(
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
                        if (categoryKey == CategoryKey.FOR_YOU && keyword.isNullOrBlank()) {
                            database.cachedFeedDao().getByFeedKey(feedKey)
                        } else {
                            val matchTerms = mutableListOf<String>()

                            if (categoryKey != CategoryKey.FOR_YOU) {
                                if (keywords.isNotEmpty()) {
                                    val categoryMatch = "(" + keywords.joinToString(" OR ") { "\"$it\"" } + ")"
                                    matchTerms.add(categoryMatch)
                                } else {
                                    matchTerms.add("(\"${categoryKey.value}\")")
                                }
                            }

                            if (!keyword.isNullOrBlank()) {
                                val sanitizedKeyword = keyword.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
                                if (sanitizedKeyword.isNotEmpty()) {
                                    val words = sanitizedKeyword.split("\\s+".toRegex())
                                    val prefixQuery = words.joinToString(" ") { "$it*" }
                                    matchTerms.add("($prefixQuery)")
                                }
                            }

                            val matchQuery = matchTerms.joinToString(" AND ")
                            
                            if (matchQuery.isNotEmpty()) {
                                database.cachedFeedDao().getFilteredFeedWithMatch(matchQuery, source)
                            } else {
                                database.cachedFeedDao().getFilteredFeedWithoutMatch(source)
                            }
                        }
                    }
                ).flow.map { pagingData ->
                    pagingData.map { it.toDomainArticle() }
                }
            }
    }

    override fun getAvailableSources(): kotlinx.coroutines.flow.Flow<List<String>> {
        return database.cachedFeedDao().getAvailableSources()
    }

    override suspend fun cachedArticleByUrl(url: String): Article? {
        return database.cachedFeedDao().findOneByUrl(url)?.toDomainArticle()
    }


    override suspend fun getTrendingTopics(): Result<List<TrendingTopic>> {
        if (!connectivityMonitor.isOnline()) {
            return Result.failure(IOException("Offline"))
        }
        return try {
            val response = pulseBackendApi.getTrendingTopics()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                Result.success(body.map { it.toDomain() })
            } else {
                Result.failure(retrofit2.HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNewsMeta(): Result<com.example.newsapp.data.remote.dto.PulseMetaDto> {
        if (!connectivityMonitor.isOnline()) {
            return Result.failure(IOException("Offline"))
        }
        return try {
            val response = pulseBackendApi.getNewsMeta()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
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
                val newScore = feedScorer.computeScore(
                    title = entity.title,
                    content = entity.content,
                    description = entity.description,
                    sourceName = entity.sourceName,
                    sourceTier = entity.sourceTier,
                    publishedAt = entity.publishedAt,
                    userWeights = currentPrefs,
                    currentTimeMillis = currentTime
                )
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
