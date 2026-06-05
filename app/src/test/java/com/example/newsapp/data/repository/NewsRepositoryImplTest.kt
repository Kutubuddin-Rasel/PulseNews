package com.example.newsapp.data.repository

import androidx.room.withTransaction
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.Room.ArticleDatabase
import com.example.newsapp.Room.CachedFeedArticleEntity
import com.example.newsapp.Room.CachedFeedDao
import com.example.newsapp.data.remote.dto.PulseArticleDto
import com.example.newsapp.data.remote.dto.ProvenanceDto
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.domain.model.VerificationStatus
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.domain.util.FeedScorer
import com.example.newsapp.module.Article
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NewsRepositoryImplTest {

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

    @Before
    fun setup() {
        pulseBackendApi = mockk()
        database = mockk()
        cachedFeedDao = mockk(relaxed = true)
        taxonomyRepository = mockk(relaxed = true)
        algoPrefsRepo = mockk()
        connectivityMonitor = mockk()
        clockProvider = mockk()
        telemetry = mockk(relaxed = true)
        feedScorer = mockk()

        every { database.cachedFeedDao() } returns cachedFeedDao
        every { connectivityMonitor.isOnline() } returns true
        every { clockProvider.nowMillis() } returns 1000L
        every { algoPrefsRepo.preferences } returns flowOf(emptyMap<String, Float>())
        every { feedScorer.computeScore(any(), any(), any()) } returns 1.0f

        // Mock Room's withTransaction extension function
        mockkStatic(
            "androidx.room.RoomDatabaseKt"
        )
        coEvery { database.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            val block = arg<suspend () -> Unit>(1)
            block.invoke()
        }

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
    fun `syncFirehose maps items successfully without dropping malformed nodes and saves to DB`() = runTest {
        // Arrange
        val dtoList = listOf(
            PulseArticleDto(
                id = "1",
                title = "Valid Article",
                link = "https://example.com/1",
                snippet = "Snippet",
                pubDate = "2023-01-01T00:00:00Z",
                source = "Source",
                provenance = ProvenanceDto(
                    status = "SOURCE_VERIFIED",
                    verificationMethod = "method",
                    trustedSigner = "signer"
                )
            ),
            PulseArticleDto(
                id = "2",
                title = "Malformed Status Article",
                link = "https://example.com/2",
                snippet = "Snippet",
                pubDate = "2023-01-02T00:00:00Z",
                source = "Source",
                provenance = ProvenanceDto(
                    status = "INVALID_UNKNOWN_STATUS",
                    verificationMethod = "method",
                    trustedSigner = "signer"
                )
            )
        )
        
        coEvery { pulseBackendApi.getNewsFeed(null, 400) } returns Response.success(dtoList)

        val insertedEntities = slot<List<CachedFeedArticleEntity>>()
        coEvery { cachedFeedDao.upsertAll(capture(insertedEntities)) } returns Unit

        // Act
        val result = repository.syncFirehose()

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { cachedFeedDao.clearFeed("firehose") }
        coVerify(exactly = 1) { cachedFeedDao.upsertAll(any()) }

        val entities = insertedEntities.captured
        assertEquals(2, entities.size)

        // First article should be VERIFIED
        assertEquals("SOURCE_VERIFIED", entities[0].verificationStatus)
        
        // Second article should fallback to UNVERIFIED instead of crashing/dropping
        assertEquals("UNVERIFIED", entities[1].verificationStatus)
    }
}
