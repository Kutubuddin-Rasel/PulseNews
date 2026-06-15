package com.example.newsapp.ViewModel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsapp.domain.repository.RecentSearchRepository
import com.example.newsapp.domain.model.TrendingTopic
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    val searchQuery: String
    val trendingTopics: List<TrendingTopic>
    val recentSearches: List<String>

    data class Content(
        override val searchQuery: String = "",
        override val trendingTopics: List<TrendingTopic> = emptyList(),
        override val recentSearches: List<String> = emptyList()
    ) : SearchUiState
}

sealed interface SearchEvent {
    data class OnQueryChange(val newQuery: String) : SearchEvent
    data object ClearQuery : SearchEvent
    data class RecordRecentSearch(val query: String) : SearchEvent
    data object ClearRecentSearches : SearchEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchNewsUseCase: com.example.newsapp.domain.usecase.news.SearchNewsUseCase,
    private val getTrendingTopicsUseCase: com.example.newsapp.domain.usecase.news.GetTrendingTopicsUseCase,
    private val recentSearchRepository: RecentSearchRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val state: StateFlow<SearchUiState> = kotlinx.coroutines.flow.combine(
        _searchQuery,
        getTrendingTopicsUseCase(),
        recentSearchRepository.getRecentSearches()
    ) { query, trending, recent ->
        SearchUiState.Content(
            searchQuery = query,
            trendingTopics = trending,
            recentSearches = recent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Content())

    val searchResults: Flow<PagingData<Article>> = _searchQuery
        .debounce(400)
        .distinctUntilChanged()
        .filter { it.isNotBlank() }
        .flatMapLatest { query -> searchNewsUseCase(query) }
        .cachedIn(viewModelScope)

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> _searchQuery.value = event.newQuery
            is SearchEvent.ClearQuery -> _searchQuery.value = ""
            is SearchEvent.RecordRecentSearch -> recordRecentSearch(event.query)
            is SearchEvent.ClearRecentSearches -> clearRecentSearches()
        }
    }

    private fun recordRecentSearch(query: String) {
        viewModelScope.launch {
            recentSearchRepository.recordRecentSearch(query)
        }
    }

    private fun clearRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.clearRecentSearches()
        }
    }
}
