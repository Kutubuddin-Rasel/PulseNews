package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.decodeNavUrl
import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository,
    private val savedArticleRepository: SavedArticleRepository,
    private val appTelemetry: AppTelemetry
) : ViewModel() {

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState

    private val encodedUrl: String = savedStateHandle.get<String>("url").orEmpty()
    val decodedUrl: String = decodeNavUrl(encodedUrl)

    val article: StateFlow<Article?> = flow {
        val fetched = savedArticleRepository.articleByUrl(decodedUrl)
            ?: newsRepository.cachedArticleByUrl(decodedUrl)
        emit(fetched)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _isSavedMutable = MutableStateFlow<Boolean?>(null)

    val isSaved: StateFlow<Boolean> = flow {
        val initialSaved = savedArticleRepository.isSaved(decodedUrl)
        _isSavedMutable.value = initialSaved
        _isSavedMutable.collect { if (it != null) emit(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    // Staff Engineer: Opposing Views / Filter Bubble Bursting
    // We dynamically extract keywords from the article title and trigger an 'everything' query
    // sorting by relevancy to fetch alternative perspectives from other publishers.
    @OptIn(ExperimentalCoroutinesApi::class)
    val relatedPerspectives: kotlinx.coroutines.flow.Flow<PagingData<Article>> = article
        .filterNotNull()
        .distinctUntilChangedBy { it.url }
        .flatMapLatest { currentArticle ->
            val keywords = extractCoreKeywords(currentArticle.title)
            newsRepository.getFeed(categoryKey = CategoryKey.FOR_YOU, keyword = keywords)
        }.cachedIn(viewModelScope)

    companion object {
        private val STOP_WORDS = setOf("the", "and", "is", "in", "it", "to", "of", "for", "on", "with", "as", "by", "at", "an", "be", "this", "that")
        private val CLEANUP_REGEX = Regex("[^a-z0-9\\s]")
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }

    private fun extractCoreKeywords(title: String): String {
        val words = title.lowercase().replace(CLEANUP_REGEX, "").split(WHITESPACE_REGEX)
        val significantWords = words.filter { it.length > 3 && it !in STOP_WORDS }
        return significantWords.take(3).joinToString(" ").ifEmpty { "politics" } // fallback
    }

    fun logInteraction(interactionType: String) {
        article.value?.backendId?.let { id ->
            appTelemetry.logInteraction(id, interactionType)
        }
    }

    fun logRelatedInteraction(relatedBackendId: String, interactionType: String) {
        appTelemetry.logInteraction(relatedBackendId, interactionType)
    }

    fun toggleSaved() {
        if (_isSavedMutable.value == null) return
        viewModelScope.launch {
            val item = article.value
            if (item == null) {
                _events.emit(UiEvent.Generic("Unable to resolve this article."))
                return@launch
            }

            if (_isSavedMutable.value == true) {
                savedArticleRepository.deleteArticle(item)
                _isSavedMutable.value = false
                _events.emit(UiEvent.Generic("Removed from saved"))
            } else {
                savedArticleRepository.saveArticle(item)
                _isSavedMutable.value = true
                _events.emit(UiEvent.Saved())
            }
        }
    }

    fun saveOnly() {
        if (_isSavedMutable.value == null) return
        viewModelScope.launch {
            val item = article.value
            if (item == null) {
                _events.emit(UiEvent.Generic("Unable to resolve this article."))
                return@launch
            }

            if (_isSavedMutable.value == true) {
                _events.emit(UiEvent.AlreadySaved())
            } else {
                savedArticleRepository.saveArticle(item)
                _isSavedMutable.value = true
                _events.emit(UiEvent.Saved())
            }
        }
    }
}
