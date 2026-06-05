package com.example.newsapp.ViewModel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsapp.Hilt.EngagementDataStore
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    @EngagementDataStore private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _trendingTopics = MutableStateFlow<List<TrendingTopic>>(emptyList())
    val trendingTopics: StateFlow<List<TrendingTopic>> = _trendingTopics

    val recentSearches: StateFlow<List<String>> = dataStore.data
        .map { prefs ->
            prefs[RECENT_SEARCHES_KEY]
                ?.split(RECENT_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: Flow<PagingData<Article>> = _searchQuery
        .debounce(400)
        .distinctUntilChanged()
        .filter { it.isNotBlank() }
        .flatMapLatest { query -> newsRepository.searchNews(query) }
        .cachedIn(viewModelScope)

    init {
        fetchTrendingTopics()
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearQuery() {
        _searchQuery.value = ""
    }

    fun recordRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_RECENT_LENGTH) return
        viewModelScope.launch {
            dataStore.edit { prefs ->
                val current = prefs[RECENT_SEARCHES_KEY]
                    ?.split(RECENT_SEPARATOR)
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                val deduped = current.filterNot { it.equals(trimmed, ignoreCase = true) }
                val updated = (listOf(trimmed) + deduped).take(MAX_RECENT_SEARCHES)
                prefs[RECENT_SEARCHES_KEY] = updated.joinToString(RECENT_SEPARATOR)
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs.remove(RECENT_SEARCHES_KEY) }
        }
    }

    private fun fetchTrendingTopics() {
        viewModelScope.launch {
            try {
                val response = newsRepository.getTrendingTopics()
                if (response.isSuccess) {
                    _trendingTopics.value = response.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                // Trending is non-critical; suppress from UI but log for observability.
                android.util.Log.e("SearchViewModel", "Failed to fetch trending topics", e)
            }
        }
    }

    companion object {
        private const val MAX_RECENT_SEARCHES = 8
        private const val MIN_RECENT_LENGTH = 2
        private const val RECENT_SEPARATOR = "\u0001" // SOH — never appears in user queries
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
    }
}
