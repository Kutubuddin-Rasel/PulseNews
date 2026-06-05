package com.example.newsapp.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.Room.ArticleDatabase
import com.example.newsapp.Room.CachedFeedArticleEntity
import com.example.newsapp.Room.CachedFeedDao
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.data.repository.AlgorithmPreferencesRepository
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.domain.util.FeedScorer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class NewsRepositoryCategoryFilterTest {

    private lateinit var pulseBackendApi: PulseBackendApi
    private lateinit var database: ArticleDatabase
    private lateinit var cachedFeedDao: CachedFeedDao
    private lateinit var taxonomyRepository: TaxonomyRepository
    private lateinit var algoPrefsRepo: AlgorithmPreferencesRepository
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var clockProvider: ClockProvider
    private lateinit var telemetry: AppTelemetry
    private lateinit var feedScorer: FeedScorer

    private lateinit var repository: NewsRepositoryImpl

    private val fakePagingSource = object : PagingSource<Int, CachedFeedArticleEntity>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CachedFeedArticleEntity> = LoadResult.Page(emptyList(), null, null)
        override fun getRefreshKey(state: PagingState<Int, CachedFeedArticleEntity>): Int? = null
    }

    @Before
    fun setup() {
        pulseBackendApi = mockk(relaxed = true)
        database = mockk()
        cachedFeedDao = mockk(relaxed = true)
        taxonomyRepository = mockk(relaxed = true)
        algoPrefsRepo = mockk()
        connectivityMonitor = mockk(relaxed = true)
        clockProvider = mockk(relaxed = true)
        telemetry = mockk(relaxed = true)
        feedScorer = mockk(relaxed = true)

        every { database.cachedFeedDao() } returns cachedFeedDao
        every { connectivityMonitor.isOnline() } returns true
        every { algoPrefsRepo.preferences } returns flowOf(emptyMap<String, Float>())
        every { taxonomyRepository.dictionaryFlow } returns flowOf(mapOf("tech" to listOf("AI", "Apple"), "sports" to listOf("Soccer")))

        repository = NewsRepositoryImpl(
            pulseBackendApi = pulseBackendApi,
            database = database,
            taxonomyRepository = taxonomyRepository,
            algoPrefsRepo = algoPrefsRepo,
            connectivityMonitor = connectivityMonitor,
            clockProvider = clockProvider,
            telemetry = telemetry,
            feedScorer = feedScorer
        )
    }

    @Test
    fun `getFeed with categoryId 2 translates to tech category filter`() = runTest {
        // Arrange
        // For categoryId=2 (tech) and no keyword, it should call getFilteredFeedWithMatch with category="tech"
        // Wait, since dictionaryFlow has "tech", matchQuery won't be empty, it will call getFilteredFeedWithMatch
        every { cachedFeedDao.getFilteredFeedWithMatch(any(), any()) } returns fakePagingSource

        // Act
        val flow = repository.getFeed(categoryId = 2)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect()
        }

        // Assert
        // We just need to verify what was called on DAO.
        io.mockk.verify(timeout = 1000) {
            cachedFeedDao.getFilteredFeedWithMatch(
                matchQuery = match { it.contains("\"AI\"") && it.contains("\"Apple\"") },
                source = null
            )
        }
        job.cancel()
    }

    @Test
    fun `getFeed with categoryId 5 translates to sports category filter`() = runTest {
        // Arrange
        every { cachedFeedDao.getFilteredFeedWithMatch(any(), any()) } returns fakePagingSource

        // Act
        val flow = repository.getFeed(categoryId = 5)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect()
        }

        // Assert
        io.mockk.verify(timeout = 1000) {
            cachedFeedDao.getFilteredFeedWithMatch(
                matchQuery = match { it.contains("\"Soccer\"") },
                source = null
            )
        }
        job.cancel()
    }

    @Test
    fun `getFeed with categoryId 1 translates to For You (null category)`() = runTest {
        // Arrange
        every { cachedFeedDao.getByFeedKey("for_you") } returns fakePagingSource

        // Act
        val flow = repository.getFeed(categoryId = 1, source = null)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect()
        }

        // Assert
        io.mockk.verify(timeout = 1000) {
            cachedFeedDao.getByFeedKey("for_you")
        }
        job.cancel()
    }

    @Test
    fun `getFeed with categoryId 3 (business) without dictionary match translates to category match`() = runTest {
        // Arrange
        every { cachedFeedDao.getFilteredFeedWithMatch(any(), any()) } returns fakePagingSource

        // Act
        val flow = repository.getFeed(categoryId = 3)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect()
        }

        // Assert
        io.mockk.verify(timeout = 1000) {
            cachedFeedDao.getFilteredFeedWithMatch(
                matchQuery = match { it.contains("\"business\"") },
                source = null
            )
        }
        job.cancel()
    }
}
