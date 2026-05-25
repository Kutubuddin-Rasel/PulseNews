package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed interface SavedUiEvent {
    data class UndoDelete(val article: Article) : SavedUiEvent
    data class Message(val value: String) : SavedUiEvent
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ImmutableList<Article>>>(UiState.Loading)
    val state: StateFlow<UiState<ImmutableList<Article>>> = _state

    private val _events = MutableSharedFlow<SavedUiEvent>()
    val events: SharedFlow<SavedUiEvent> = _events

    init {
        viewModelScope.launch {
            savedArticleRepository.observeSavedArticles().collectLatest { saved ->
                _state.value = if (saved.isEmpty()) {
                    UiState.Empty("No saved articles yet.")
                } else {
                    UiState.Success(saved.toImmutableList())
                }
            }
        }
        
        // Trigger lazy backend reconciliation on screen load
        viewModelScope.launch {
            savedArticleRepository.syncBookmarks()
        }
    }

    fun delete(article: Article) {
        viewModelScope.launch {
            runCatching {
                savedArticleRepository.deleteArticle(article)
            }.onSuccess {
                _events.emit(SavedUiEvent.UndoDelete(article))
            }.onFailure {
                _events.emit(SavedUiEvent.Message("Failed to remove article."))
            }
        }
    }

    fun undoDelete(article: Article) {
        viewModelScope.launch {
            savedArticleRepository.saveArticle(article)
            _events.emit(SavedUiEvent.Message("Article restored"))
        }
    }
}
