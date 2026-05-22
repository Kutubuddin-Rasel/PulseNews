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

enum class FeedMode {
    Top,
    Everything
}

data class FilterUiState(
    val mode: FeedMode = FeedMode.Top,
    val category: String = "science",
    val queryInput: String = "politics",
    val activeQuery: String = "politics",
    val sortBy: String = "relevancy"
)

data class HomeUiState(
    val filter: FilterUiState = FilterUiState(),
    val isRefreshing: Boolean = false,
    val event: String? = null
)

import com.example.newsapp.data.repository.PrivacyPreferencesRepository
import com.example.newsapp.data.repository.AlgorithmPreferencesRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val algoPrefsRepo: AlgorithmPreferencesRepository,
    private val privacyPrefsRepo: PrivacyPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            filter = FilterUiState(
                mode = FeedMode.valueOf(savedStateHandle[KEY_MODE] ?: FeedMode.Top.name),
                category = savedStateHandle[KEY_CATEGORY] ?: "science",
                queryInput = savedStateHandle[KEY_QUERY_INPUT] ?: "politics",
                activeQuery = savedStateHandle[KEY_ACTIVE_QUERY] ?: "politics",
                sortBy = savedStateHandle[KEY_SORT_BY] ?: "relevancy"
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

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
            newsRepository.topHeadlines(category = "")
        }
        .cachedIn(viewModelScope)

    fun switchMode(mode: FeedMode) {
        val current = _uiState.value.filter
        if (current.mode == mode) return
        updateFilter(current.copy(mode = mode))
    }

    fun setCategory(category: String) {
        val current = _uiState.value.filter
        if (current.category == category && current.mode == FeedMode.Top) return
        updateFilter(current.copy(mode = FeedMode.Top, category = category))
    }

    fun updateQueryInput(text: String) {
        updateFilter(_uiState.value.filter.copy(queryInput = text))
    }

    fun submitSearch() {
        val current = _uiState.value.filter
        val textQuery = current.queryInput.trim()
        
        val resolved = if (textQuery.isNotEmpty()) {
            textQuery
        } else {
            // Staff Engineer: Multi-Armed Bandit / Thompson Sampling style random weighted selection
            // If the user didn't type anything, we use their preferred algorithm weights
            val weights = algoPrefs ?: mapOf("technology" to 0.2f, "politics" to 0.2f, "general" to 0.2f, "business" to 0.2f, "health" to 0.2f)
            selectWeightedTopic(weights)
        }
        updateFilter(current.copy(mode = FeedMode.Everything, activeQuery = resolved))
    }

    private var algoPrefs: Map<String, Float>? = null

    private fun selectWeightedTopic(weights: Map<String, Float>): String {
        val totalWeight = weights.values.sum()
        if (totalWeight <= 0f) return "politics"
        var random = Math.random() * totalWeight
        for ((topic, weight) in weights) {
            random -= weight
            if (random <= 0.0) return topic
        }
        return weights.keys.firstOrNull() ?: "politics"
    }

    fun setSortBy(sortBy: String) {
        val current = _uiState.value.filter
        if (current.sortBy == sortBy && current.mode == FeedMode.Everything) return
        updateFilter(current.copy(mode = FeedMode.Everything, sortBy = sortBy))
    }

    private fun updateFilter(filter: FilterUiState) {
        savedStateHandle[KEY_MODE] = filter.mode.name
        savedStateHandle[KEY_CATEGORY] = filter.category
        savedStateHandle[KEY_QUERY_INPUT] = filter.queryInput
        savedStateHandle[KEY_ACTIVE_QUERY] = filter.activeQuery
        savedStateHandle[KEY_SORT_BY] = filter.sortBy
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    companion object {
        private const val KEY_MODE = "home_mode"
        private const val KEY_CATEGORY = "home_category"
        private const val KEY_QUERY_INPUT = "home_query_input"
        private const val KEY_ACTIVE_QUERY = "home_active_query"
        private const val KEY_SORT_BY = "home_sort_by"
    }
}
