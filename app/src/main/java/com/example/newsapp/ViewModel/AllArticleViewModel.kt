package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AllArticleViewModel @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) : ViewModel() {

    val state: StateFlow<UiState<List<Article>>> = savedArticleRepository.observeSavedArticles()
        .map { articles ->
            if (articles.isEmpty()) {
                UiState.Empty("No saved articles yet.")
            } else {
                UiState.Success(articles)
            }
        }
        .catch {
            emit(UiState.Error("Failed to load saved articles.", retryable = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )
}
