package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.TrendingTopic
import com.example.newsapp.domain.repository.NewsRepository
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
import com.example.newsapp.data.repository.PrivacyPreferencesRepository
import com.example.newsapp.data.repository.AlgorithmPreferencesRepository
import com.example.newsapp.data.util.AuthManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.newsapp.domain.repository.SavedArticleRepository

import com.example.newsapp.domain.model.CategoryKey

enum class FeedMode { FOR_YOU, TRENDING }

data class FilterUiState(
    val categoryKey: CategoryKey = CategoryKey.FOR_YOU,
    val queryInput: String = "",
    val activeQuery: String = "",
    val selectedSource: String? = null
)

data class HomeUiState(
    val filter: FilterUiState = FilterUiState(),
    val isRefreshing: Boolean = false,
    val event: String? = null
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val savedArticleRepository: SavedArticleRepository,
    private val algoPrefsRepo: AlgorithmPreferencesRepository,
    private val privacyPrefsRepo: PrivacyPreferencesRepository,
    private val localEngagementTracker: com.example.newsapp.data.util.LocalEngagementTracker,
    private val savedStateHandle: SavedStateHandle,
    private val appTelemetry: com.example.newsapp.data.util.AppTelemetry,
    private val authManager: AuthManager,
    private val taxonomyRepository: com.example.newsapp.data.repository.TaxonomyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            filter = FilterUiState(
                categoryKey = CategoryKey(savedStateHandle.get<String>(KEY_CATEGORY_KEY) ?: "for_you"),
                queryInput = savedStateHandle[KEY_QUERY_INPUT] ?: "",
                activeQuery = savedStateHandle[KEY_ACTIVE_QUERY] ?: "",
                selectedSource = savedStateHandle[KEY_SELECTED_SOURCE]
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    val availableSources: StateFlow<List<String>> = newsRepository.getAvailableSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dynamicCategories: StateFlow<List<Pair<CategoryKey, String>>> = taxonomyRepository.dictionaryFlow
        .map { dict ->
            val list = mutableListOf(CategoryKey.FOR_YOU to "For You")
            dict.keys.forEach { key ->
                val displayName = key.value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                list.add(key to displayName)
            }
            list
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(CategoryKey.FOR_YOU to "For You"))

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events
    
    private val _telemetryConsent = MutableStateFlow<Boolean?>(true)
    val telemetryConsent: StateFlow<Boolean?> = _telemetryConsent

    private val _trendingTopics = MutableStateFlow<List<TrendingTopic>>(emptyList())
    val trendingTopics: StateFlow<List<TrendingTopic>> = _trendingTopics

    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated: StateFlow<String?> = _lastUpdated

    val savedArticles: StateFlow<Set<String>> = savedArticleRepository.observeSavedArticles()
        .map { articles -> articles.map { it.url }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val isAuthenticated: StateFlow<Boolean> = authManager.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val searchQueryFlow = MutableStateFlow(savedStateHandle[KEY_QUERY_INPUT] ?: "")

    init {
        viewModelScope.launch {
            algoPrefsRepo.preferences.collectLatest { prefs ->
                algoPrefs = prefs
            }
        }
        viewModelScope.launch {
            privacyPrefsRepo.telemetryConsent.collectLatest { consent ->
                _telemetryConsent.value = consent
            }
        }
        
        fetchNewsMeta()
        
        viewModelScope.launch {
            try {
                val response = newsRepository.getTrendingTopics()
                if (response.isSuccess) {
                    _trendingTopics.value = response.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                // Ignore failure for trending topics, it's non-critical
            }
        }

        viewModelScope.launch {
            searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val current = _uiState.value.filter
                    val newActiveQuery = query.trim()
                    if (current.activeQuery != newActiveQuery) {
                        updateFilter(current.copy(activeQuery = newActiveQuery))
                    }
                }
        }
    }

    fun setTelemetryConsent(granted: Boolean) {
        viewModelScope.launch {
            privacyPrefsRepo.setConsent(granted)
        }
    }

    fun fetchNewsMeta() {
        viewModelScope.launch {
            try {
                val result = newsRepository.getNewsMeta()
                if (result.isSuccess) {
                    _lastUpdated.value = result.getOrNull()?.lastUpdated
                }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    private data class FeedCacheKey(val filter: FilterUiState, val isAuthenticated: Boolean)
    private val feedCache = mutableMapOf<FeedCacheKey, Flow<PagingData<Article>>>()

    val feed: Flow<PagingData<Article>> get() {
        val filter = _uiState.value.filter
        val isAuth = isAuthenticated.value
        val key = FeedCacheKey(filter, isAuth)
        return feedCache.getOrPut(key) {
            if (key.filter.categoryKey == CategoryKey.FOR_YOU && !key.isAuthenticated) {
                // Zero-Trust Auth Gate: Unauthenticated users get an empty state on "For You"
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                newsRepository.getFeed(
                    categoryKey = key.filter.categoryKey,
                    keyword = key.filter.activeQuery.takeIf { it.isNotBlank() },
                    source = key.filter.selectedSource
                ).cachedIn(viewModelScope)
            }
        }
    }



    fun signIn(activityContext: android.content.Context) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(activityContext)
            if (result.isFailure) {
                _events.emit("Sign in failed. Ensure you have a Google Account on this device.")
            }
        }
    }

    fun setCategory(categoryKey: CategoryKey) {
        val current = _uiState.value.filter
        if (current.categoryKey == categoryKey) return
        updateFilter(current.copy(categoryKey = categoryKey))
    }

    fun updateQueryInput(text: String) {
        searchQueryFlow.value = text
        updateFilter(_uiState.value.filter.copy(queryInput = text))
    }

    fun submitSearch() {
        val current = _uiState.value.filter
        updateFilter(current.copy(activeQuery = current.queryInput.trim()))
    }
    
    fun setSource(source: String?) {
        val current = _uiState.value.filter
        if (current.selectedSource == source) return
        updateFilter(current.copy(selectedSource = source))
    }

    private var algoPrefs: Map<String, Float>? = null

    private fun updateFilter(filter: FilterUiState) {
        savedStateHandle[KEY_CATEGORY_KEY] = filter.categoryKey.value
        savedStateHandle[KEY_QUERY_INPUT] = filter.queryInput
        savedStateHandle[KEY_ACTIVE_QUERY] = filter.activeQuery
        savedStateHandle[KEY_SELECTED_SOURCE] = filter.selectedSource
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun trackArticleClick(articleId: String) {
        val currentCategory = _uiState.value.filter.categoryKey
        viewModelScope.launch {
            localEngagementTracker.incrementClick(currentCategory)
            appTelemetry.logInteraction(articleId, "article_clicked")
        }
    }

    fun saveArticle(article: Article) {
        viewModelScope.launch {
            savedArticleRepository.saveArticle(article)
            _events.emit("Article saved")
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            savedArticleRepository.deleteArticle(article)
            _events.emit("Article removed")
        }
    }

    companion object {
        private const val KEY_CATEGORY_KEY = "home_category_key"
        private const val KEY_QUERY_INPUT = "home_query_input"
        private const val KEY_ACTIVE_QUERY = "home_active_query"
        private const val KEY_SELECTED_SOURCE = "home_selected_source"
    }
}
