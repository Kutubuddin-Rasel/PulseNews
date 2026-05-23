package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import androidx.paging.cachedIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class EveryNewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _sortBy = MutableStateFlow(savedStateHandle.get<String>("sort_by") ?: "relevancy")
    private val _searchInput = MutableStateFlow(savedStateHandle.get<String>("search_input") ?: "politics")
    private val _activeTopic = MutableStateFlow(savedStateHandle.get<String>("active_topic") ?: "politics")

    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<Article>> = combine(_activeTopic, _sortBy) { topic, sort ->
        EverythingQuery(topic = topic, sortBy = sort, page = 1)
    }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            newsRepository.getFeed(categoryId = 1, keyword = query.topic)
        }
        .cachedIn(viewModelScope)

    fun sort(sort: String) {
        _sortBy.value = sort
        savedStateHandle["sort_by"] = sort
    }

    fun updateTopicInput(topic: String) {
        _searchInput.value = topic
        savedStateHandle["search_input"] = topic
    }

    fun searchNow() {
        val resolvedTopic = _searchInput.value.trim().ifEmpty { "politics" }
        _activeTopic.value = resolvedTopic
        savedStateHandle["active_topic"] = resolvedTopic
    }

    fun loadNextPage() {
        // Handled by Paging 3
    }

    fun retry() {
        // Retry logic is now handled in the UI via LazyPagingItems.retry()
    }
}
