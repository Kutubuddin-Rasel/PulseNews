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
class SaveArticleViewModel @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    fun saveArticle(article: Article?) {
        if (article == null) {
            viewModelScope.launch { _events.emit(UiEvent.Generic("Unable to identify article to save.")) }
            return
        }

        viewModelScope.launch {
            val exists = savedArticleRepository.isSaved(article.url)
            if (exists) {
                _events.emit(UiEvent.AlreadySaved())
            } else {
                savedArticleRepository.saveArticle(article)
                _events.emit(UiEvent.Saved())
            }
        }
    }
}
