package com.example.newsapp.ViewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.example.newsapp.domain.util.AppTelemetry
import com.example.newsapp.domain.tracker.LocalEngagementTracker
import com.example.newsapp.domain.usecase.auth.ObserveCurrentUserUseCase
import com.example.newsapp.domain.usecase.auth.SignInUseCase
import com.example.newsapp.domain.usecase.core.GetDynamicCategoriesUseCase
import com.example.newsapp.domain.usecase.core.ObserveTelemetryConsentUseCase
import com.example.newsapp.domain.usecase.core.SetTelemetryConsentUseCase
import com.example.newsapp.domain.usecase.news.GetAvailableSourcesUseCase
import com.example.newsapp.domain.usecase.news.GetFeedUseCase
import com.example.newsapp.domain.usecase.news.ObserveFeedMetaUseCase
import com.example.newsapp.domain.usecase.saved.DeleteArticleUseCase
import com.example.newsapp.domain.usecase.saved.ObserveSavedArticlesUseCase
import com.example.newsapp.domain.usecase.saved.SaveArticleUseCase
import com.example.newsapp.module.Article
import com.example.newsapp.module.Source
import com.example.newsapp.domain.model.CategoryKey
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
import org.junit.Assert.assertNotNull
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
    private lateinit var getFeedUseCase: GetFeedUseCase
    private lateinit var getAvailableSourcesUseCase: GetAvailableSourcesUseCase
    private lateinit var observeFeedMetaUseCase: ObserveFeedMetaUseCase
    private lateinit var saveArticleUseCase: SaveArticleUseCase
    private lateinit var deleteArticleUseCase: DeleteArticleUseCase
    private lateinit var observeSavedArticlesUseCase: ObserveSavedArticlesUseCase
    private lateinit var setTelemetryConsentUseCase: SetTelemetryConsentUseCase
    private lateinit var observeTelemetryConsentUseCase: ObserveTelemetryConsentUseCase
    private lateinit var observeCurrentUserUseCase: ObserveCurrentUserUseCase
    private lateinit var signInUseCase: SignInUseCase
    private lateinit var getDynamicCategoriesUseCase: GetDynamicCategoriesUseCase
    private lateinit var localEngagementTracker: LocalEngagementTracker
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var appTelemetry: AppTelemetry

    private val authStateFlow = MutableStateFlow<FirebaseUser?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getFeedUseCase = mockk(relaxed = true)
        getAvailableSourcesUseCase = mockk(relaxed = true)
        observeFeedMetaUseCase = mockk(relaxed = true)
        saveArticleUseCase = mockk(relaxed = true)
        deleteArticleUseCase = mockk(relaxed = true)
        observeSavedArticlesUseCase = mockk(relaxed = true)
        setTelemetryConsentUseCase = mockk(relaxed = true)
        observeTelemetryConsentUseCase = mockk(relaxed = true)
        observeCurrentUserUseCase = mockk(relaxed = true)
        signInUseCase = mockk(relaxed = true)
        getDynamicCategoriesUseCase = mockk(relaxed = true)

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
        every { observeCurrentUserUseCase() } returns authStateFlow
        coEvery { getFeedUseCase(categoryKey = any(), source = any()) } returns flowOf(PagingData.from(listOf(dummyArticle)))
        coEvery { getAvailableSourcesUseCase(any()) } returns flowOf(emptyList())
        every { observeFeedMetaUseCase() } returns flowOf(null)
        every { observeTelemetryConsentUseCase() } returns flowOf(true)

        localEngagementTracker = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("home_category_id" to 1)) // For You
        appTelemetry = mockk(relaxed = true)

        viewModel = HomeViewModel(
            getFeedUseCase = getFeedUseCase,
            getAvailableSourcesUseCase = getAvailableSourcesUseCase,
            observeFeedMetaUseCase = observeFeedMetaUseCase,
            saveArticleUseCase = saveArticleUseCase,
            deleteArticleUseCase = deleteArticleUseCase,
            observeSavedArticlesUseCase = observeSavedArticlesUseCase,
            setTelemetryConsentUseCase = setTelemetryConsentUseCase,
            observeTelemetryConsentUseCase = observeTelemetryConsentUseCase,
            observeCurrentUserUseCase = observeCurrentUserUseCase,
            signInUseCase = signInUseCase,
            getDynamicCategoriesUseCase = getDynamicCategoriesUseCase,
            localEngagementTracker = localEngagementTracker,
            savedStateHandle = savedStateHandle,
            appTelemetry = appTelemetry
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pagingData is a single stream that switches with auth state (F2)`() = runTest {
        // Start unauthenticated
        authStateFlow.value = null
        advanceUntilIdle()

        // F2: the feed is one stable stream — not a per-(filter, auth) cached flow.
        val firstFlow = viewModel.pagingData

        // Sign in
        authStateFlow.value = mockk<FirebaseUser>(relaxed = true)
        advanceUntilIdle()

        val secondFlow = viewModel.pagingData

        assertNotNull(secondFlow)
        // Same instance across auth toggles: switching is internal (flatMapLatest), so there is no
        // unbounded map of cached flows accumulating one entry per visited combination.
        org.junit.Assert.assertSame(firstFlow, secondFlow)
    }
}
