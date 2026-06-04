package com.example.newsapp.ViewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.example.newsapp.data.repository.AlgorithmPreferencesRepository
import com.example.newsapp.data.repository.PrivacyPreferencesRepository
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.data.util.AuthManager
import com.example.newsapp.data.util.LocalEngagementTracker
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import com.example.newsapp.module.Source
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelAuthFlowTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var viewModel: HomeViewModel
    private lateinit var authManager: AuthManager
    private lateinit var newsRepository: NewsRepository
    private lateinit var savedArticleRepository: SavedArticleRepository
    private lateinit var algoPrefsRepo: AlgorithmPreferencesRepository
    private lateinit var privacyPrefsRepo: PrivacyPreferencesRepository
    private lateinit var localEngagementTracker: LocalEngagementTracker
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var appTelemetry: AppTelemetry

    private val authStateFlow = MutableStateFlow<FirebaseUser?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        authManager = mockk(relaxed = true)
        every { authManager.currentUser } returns authStateFlow as kotlinx.coroutines.flow.StateFlow<FirebaseUser?>

        newsRepository = mockk(relaxed = true)
        val dummyArticle = Article(
            url = "https://example.com/auth",
            backendId = "1",
            author = null,
            content = null,
            description = null,
            publishedAt = null,
            source = Source(id = null, name = "Test"),
            title = "Test Title",
            urlToImage = null
        )
        coEvery { newsRepository.getFeed(any(), any(), any()) } returns flowOf(PagingData.from(listOf(dummyArticle)))
        coEvery { newsRepository.getAvailableSources() } returns flowOf(emptyList())

        savedArticleRepository = mockk(relaxed = true)
        algoPrefsRepo = mockk(relaxed = true)
        every { algoPrefsRepo.preferences } returns flowOf(emptyMap())
        
        privacyPrefsRepo = mockk(relaxed = true)
        every { privacyPrefsRepo.telemetryConsent } returns flowOf(true)

        localEngagementTracker = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("home_category_id" to 1)) // For You
        appTelemetry = mockk(relaxed = true)

        viewModel = HomeViewModel(
            newsRepository = newsRepository,
            savedArticleRepository = savedArticleRepository,
            algoPrefsRepo = algoPrefsRepo,
            privacyPrefsRepo = privacyPrefsRepo,
            localEngagementTracker = localEngagementTracker,
            savedStateHandle = savedStateHandle,
            appTelemetry = appTelemetry,
            authManager = authManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when authentication state toggles, flow updates are not cached incorrectly`() = runTest {
        // Start unauthenticated
        authStateFlow.value = null
        advanceUntilIdle()

        // Cache the flow by reading it once
        val firstFlow = viewModel.feed

        // Sign in
        authStateFlow.value = mockk<FirebaseUser>(relaxed = true)
        advanceUntilIdle()

        // Read again, if it's cached it'll be the same empty flow reference incorrectly tied to category 1 filter state
        val secondFlow = viewModel.feed

        // We expect a new flow instance because the underlying authentication state changed,
        // but since `feedCache` caches by `FilterUiState`, it returns the old flow.
        // This test intentionally expects them to be different, exposing the bug.
        assertNotEquals(firstFlow, secondFlow)
    }
}
