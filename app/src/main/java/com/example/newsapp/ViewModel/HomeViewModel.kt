package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.TrendingTopic
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.newsapp.domain.model.CategoryKey
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

sealed interface FeedMode {
    data object FOR_YOU : FeedMode
    data object TRENDING : FeedMode
}

data class FilterUiState(
    val categoryKey: CategoryKey = CategoryKey.FOR_YOU,
    val selectedSource: String? = null
)

data class HomeUiState(
    val filter: FilterUiState = FilterUiState(),
    val isRefreshing: Boolean = false,
    val telemetryConsent: Boolean? = true,
    val isAuthenticated: Boolean = false,
    val savedArticles: Set<String> = emptySet(),
    val dynamicCategories: List<Pair<CategoryKey, String>> = listOf(CategoryKey.FOR_YOU to "For You"),
    val lastUpdated: String? = null,
    val availableSources: List<String> = emptyList(),
    val trendingTopics: List<TrendingTopic> = emptyList(),
    val snackbarMessage: String? = null
)

sealed interface HomeEvent {
    data class SetTelemetryConsent(val granted: Boolean) : HomeEvent
    data class SignIn(val activityContext: android.content.Context) : HomeEvent
    data class SetCategory(val categoryKey: CategoryKey) : HomeEvent
    data class SetSource(val source: String?) : HomeEvent
    data class TrackArticleClick(val articleId: String) : HomeEvent
    data class SaveArticle(val article: Article) : HomeEvent
    data class DeleteArticle(val article: Article) : HomeEvent
    data object SnackbarConsumed : HomeEvent
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFeedUseCase: GetFeedUseCase,
    private val getAvailableSourcesUseCase: GetAvailableSourcesUseCase,
    private val observeFeedMetaUseCase: ObserveFeedMetaUseCase,
    private val saveArticleUseCase: SaveArticleUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val observeSavedArticlesUseCase: ObserveSavedArticlesUseCase,
    private val setTelemetryConsentUseCase: SetTelemetryConsentUseCase,
    private val observeTelemetryConsentUseCase: ObserveTelemetryConsentUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val signInUseCase: SignInUseCase,
    private val getDynamicCategoriesUseCase: GetDynamicCategoriesUseCase,
    private val localEngagementTracker: com.example.newsapp.domain.tracker.LocalEngagementTracker,
    private val savedStateHandle: SavedStateHandle,
    private val appTelemetry: com.example.newsapp.domain.util.AppTelemetry
) : ViewModel() {

    private val _internalState = MutableStateFlow(
        HomeUiState(
            filter = FilterUiState(
                categoryKey = CategoryKey(savedStateHandle.get<String>(KEY_CATEGORY_KEY) ?: "for_you"),
                selectedSource = savedStateHandle[KEY_SELECTED_SOURCE]
            )
        )
    )

    // F1: the source-filter dropdown is scoped to the active feed and must switch when the category
    // changes. flatMapLatest cancels the previous category's Room query and starts the new scoped
    // one, so a stale category's sources can never race-win and contaminate the current dropdown.
    // distinctUntilChanged keeps unrelated state churn (saved set, consent, refresh) from
    // needlessly tearing down and rebuilding the DB Flow.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val availableSourcesFlow: Flow<List<String>> = _internalState
        .map { it.filter.categoryKey }
        .distinctUntilChanged()
        .flatMapLatest { categoryKey -> getAvailableSourcesUseCase(categoryKey) }

    val state: StateFlow<HomeUiState> = combine(
        combine(
            _internalState,
            observeSavedArticlesUseCase().map { articles -> articles.map { it.url }.toSet() },
            observeCurrentUserUseCase().map { it != null },
            getDynamicCategoriesUseCase(),
            availableSourcesFlow
        ) { internalState, saved, isAuth, categories, sources ->
            internalState.copy(
                savedArticles = saved,
                isAuthenticated = isAuth,
                dynamicCategories = categories,
                availableSources = sources
            )
        },
        observeTelemetryConsentUseCase(),
        observeFeedMetaUseCase()
    ) { partialState, consent, meta ->
        partialState.copy(
            telemetryConsent = consent,
            lastUpdated = meta?.lastUpdated
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        // Initialization if needed
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SetTelemetryConsent -> setTelemetryConsent(event.granted)
            is HomeEvent.SignIn -> signIn(event.activityContext)
            is HomeEvent.SetCategory -> setCategory(event.categoryKey)
            is HomeEvent.SetSource -> setSource(event.source)
            is HomeEvent.TrackArticleClick -> trackArticleClick(event.articleId)
            is HomeEvent.SaveArticle -> saveArticle(event.article)
            is HomeEvent.DeleteArticle -> deleteArticle(event.article)
            is HomeEvent.SnackbarConsumed -> _internalState.value = _internalState.value.copy(snackbarMessage = null)
        }
    }

    private fun setTelemetryConsent(granted: Boolean) {
        viewModelScope.launch {
            setTelemetryConsentUseCase(granted)
        }
    }

    private data class FeedCacheKey(val filter: FilterUiState, val isAuthenticated: Boolean)

    // F2: a single paging stream that switches with the active filter/auth (the canonical Paging
    // pattern) and is cached exactly once. Replaces an unbounded per-(filter, auth) map of
    // `cachedIn` flows that was never evicted — it pinned one flow per visited combination for the
    // ViewModel's whole life. `distinctUntilChanged` collapses unrelated state emissions (saved
    // set, consent, …) so the feed only re-subscribes when filter/auth actually changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<Article>> = state
        .map { FeedCacheKey(it.filter, it.isAuthenticated) }
        .distinctUntilChanged()
        .flatMapLatest { key ->
            if (key.filter.categoryKey == CategoryKey.FOR_YOU && !key.isAuthenticated) {
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                getFeedUseCase(
                    categoryKey = key.filter.categoryKey,
                    source = key.filter.selectedSource
                )
            }
        }
        .cachedIn(viewModelScope)

    private fun signIn(activityContext: android.content.Context) {
        viewModelScope.launch {
            val result = signInUseCase(activityContext)
            if (result.isFailure) {
                _internalState.value = _internalState.value.copy(snackbarMessage = "Sign in failed. Ensure you have a Google Account on this device.")
            }
        }
    }

    private fun setCategory(categoryKey: CategoryKey) {
        val current = _internalState.value.filter
        if (current.categoryKey == categoryKey) return
        updateFilter(current.copy(categoryKey = categoryKey))
    }

    private fun setSource(source: String?) {
        val current = _internalState.value.filter
        if (current.selectedSource == source) return
        updateFilter(current.copy(selectedSource = source))
    }

    private fun updateFilter(filter: FilterUiState) {
        savedStateHandle[KEY_CATEGORY_KEY] = filter.categoryKey.value
        savedStateHandle[KEY_SELECTED_SOURCE] = filter.selectedSource
        _internalState.value = _internalState.value.copy(filter = filter)
    }

    private fun trackArticleClick(articleId: String) {
        val currentCategory = _internalState.value.filter.categoryKey
        viewModelScope.launch {
            localEngagementTracker.incrementClick(currentCategory)
            appTelemetry.trackClick(articleId)
        }
    }

    private fun saveArticle(article: Article) {
        viewModelScope.launch {
            saveArticleUseCase(article)
            _internalState.value = _internalState.value.copy(snackbarMessage = "Article saved")
        }
    }

    private fun deleteArticle(article: Article) {
        viewModelScope.launch {
            deleteArticleUseCase(article)
            _internalState.value = _internalState.value.copy(snackbarMessage = "Article removed")
        }
    }

    companion object {
        private const val KEY_CATEGORY_KEY = "home_category_key"
        private const val KEY_SELECTED_SOURCE = "home_selected_source"
    }
}
