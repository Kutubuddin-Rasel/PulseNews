package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteArticleViewModel @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            runCatching {
                savedArticleRepository.deleteArticle(article)
            }.onFailure {
                _events.emit(UiEvent.DeleteFailed())
            }
        }
    }
}
