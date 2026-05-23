package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsapp.domain.model.EverythingQuery
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
import javax.inject.Inject

enum class FeedMode { FOR_YOU, TRENDING }

data class FilterUiState(
    val categoryId: Int = 1,
    val queryInput: String = "",
    val activeQuery: String = "",
    val selectedSource: String? = null
)

data class HomeUiState(
    val filter: FilterUiState = FilterUiState(),
    val isRefreshing: Boolean = false,
    val event: String? = null
)

import com.example.newsapp.data.repository.PrivacyPreferencesRepository
import com.example.newsapp.data.repository.AlgorithmPreferencesRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val algoPrefsRepo: AlgorithmPreferencesRepository,
    private val privacyPrefsRepo: PrivacyPreferencesRepository,
    private val localEngagementTracker: com.example.newsapp.data.util.LocalEngagementTracker,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            filter = FilterUiState(
                categoryId = savedStateHandle[KEY_CATEGORY_ID] ?: 1,
                queryInput = savedStateHandle[KEY_QUERY_INPUT] ?: "",
                activeQuery = savedStateHandle[KEY_ACTIVE_QUERY] ?: "",
                selectedSource = savedStateHandle[KEY_SELECTED_SOURCE]
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    val availableSources: StateFlow<List<String>> = newsRepository.getAvailableSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events
    
    private val _telemetryConsent = MutableStateFlow<Boolean?>(true)
    val telemetryConsent: StateFlow<Boolean?> = _telemetryConsent

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
        
        // Active Foreground Poller for Real-Time Edge Delivery
        viewModelScope.launch {
            var lastKnownUpdatedTime: String? = null
            while (true) {
                val metaResult = newsRepository.getNewsMetaLastUpdated()
                if (metaResult.isSuccess) {
                    val currentUpdatedTime = metaResult.getOrNull()
                    if (currentUpdatedTime != null) {
                        if (lastKnownUpdatedTime == null) {
                            // First run, just store it
                            lastKnownUpdatedTime = currentUpdatedTime
                        } else if (lastKnownUpdatedTime != currentUpdatedTime) {
                            // Backend has new data! Trigger headless sync
                            lastKnownUpdatedTime = currentUpdatedTime
                            newsRepository.syncFirehose()
                        }
                    }
                }
                // Poll every 30 seconds
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    fun setTelemetryConsent(granted: Boolean) {
        viewModelScope.launch {
            privacyPrefsRepo.setConsent(granted)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val feed: Flow<PagingData<Article>> = _uiState
        .map { it.filter }
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            newsRepository.getFeed(
                categoryId = filter.categoryId,
                keyword = filter.activeQuery.takeIf { it.isNotBlank() },
                source = filter.selectedSource
            )
        }
        .cachedIn(viewModelScope)

    fun setCategory(categoryId: Int) {
        val current = _uiState.value.filter
        if (current.categoryId == categoryId) return
        updateFilter(current.copy(categoryId = categoryId))
    }

    fun updateQueryInput(text: String) {
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
        savedStateHandle[KEY_CATEGORY_ID] = filter.categoryId
        savedStateHandle[KEY_QUERY_INPUT] = filter.queryInput
        savedStateHandle[KEY_ACTIVE_QUERY] = filter.activeQuery
        savedStateHandle[KEY_SELECTED_SOURCE] = filter.selectedSource
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun trackArticleClick() {
        val currentCategory = _uiState.value.filter.categoryId
        viewModelScope.launch {
            localEngagementTracker.incrementClick(currentCategory)
        }
    }

    companion object {
        private const val KEY_CATEGORY_ID = "home_category_id"
        private const val KEY_QUERY_INPUT = "home_query_input"
        private const val KEY_ACTIVE_QUERY = "home_active_query"
        private const val KEY_SELECTED_SOURCE = "home_selected_source"
    }
}
