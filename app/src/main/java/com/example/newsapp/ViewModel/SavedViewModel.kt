package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.usecase.saved.DeleteArticleUseCase
import com.example.newsapp.domain.usecase.saved.ObserveSavedArticlesUseCase
import com.example.newsapp.domain.usecase.saved.SaveArticleUseCase
import com.example.newsapp.domain.usecase.saved.SyncBookmarksUseCase
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class SavedUiState(
    val articles: UiState<ImmutableList<Article>> = UiState.Loading,
    val snackbarMessage: String? = null,
    val deletedArticleForUndo: Article? = null
)

sealed interface SavedEvent {
    data class Delete(val article: Article) : SavedEvent
    data class UndoDelete(val article: Article) : SavedEvent
    data object SnackbarDismissed : SavedEvent
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val observeSavedArticlesUseCase: ObserveSavedArticlesUseCase,
    private val syncBookmarksUseCase: SyncBookmarksUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val saveArticleUseCase: SaveArticleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState

    init {
        viewModelScope.launch {
            observeSavedArticlesUseCase().collectLatest { saved ->
                _uiState.value = _uiState.value.copy(
                    articles = if (saved.isEmpty()) {
                        UiState.Empty("No saved articles yet.")
                    } else {
                        UiState.Success(saved.toImmutableList())
                    }
                )
            }
        }
        
        // Trigger lazy backend reconciliation on screen load
        viewModelScope.launch {
            syncBookmarksUseCase()
        }
    }

    fun onEvent(event: SavedEvent) {
        when (event) {
            is SavedEvent.Delete -> delete(event.article)
            is SavedEvent.UndoDelete -> undoDelete(event.article)
            is SavedEvent.SnackbarDismissed -> {
                _uiState.value = _uiState.value.copy(snackbarMessage = null, deletedArticleForUndo = null)
            }
        }
    }

    private fun delete(article: Article) {
        viewModelScope.launch {
            runCatching {
                deleteArticleUseCase(article)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Removed from saved",
                    deletedArticleForUndo = article
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Failed to remove article."
                )
            }
        }
    }

    private fun undoDelete(article: Article) {
        viewModelScope.launch {
            saveArticleUseCase(article)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Article restored",
                deletedArticleForUndo = null
            )
        }
    }
}
