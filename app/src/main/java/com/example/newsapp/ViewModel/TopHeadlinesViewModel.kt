package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopHeadlinesViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<Article>> = _category
        .flatMapLatest { category ->
            newsRepository.topHeadlines(category = category)
        }
        .cachedIn(viewModelScope)

    fun setCategory(category: String) {
        if (_category.value == category) return
        _category.value = category
        savedStateHandle["category"] = category
    }

    fun retry() {
        // Handled by LazyPagingItems in UI
    }

    fun loadNextPage() {
        // Handled by Paging 3
    }
}
