package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsapp.domain.util.AppTelemetry
import com.example.newsapp.decodeNavUrl
import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.domain.usecase.news.ResolveArticleUseCase
import com.example.newsapp.domain.usecase.news.SearchNewsUseCase
import com.example.newsapp.domain.usecase.saved.CheckArticleSavedUseCase
import com.example.newsapp.domain.usecase.saved.DeleteArticleUseCase
import com.example.newsapp.domain.usecase.saved.SaveArticleUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleDetailUiState(
    val article: Article? = null,
    val isSaved: Boolean = false,
    val aiState: AiState = AiState.Idle,
    val snackbarMessage: String? = null
)

sealed interface ArticleDetailEvent {
    data class LogReadDeep(val durationSeconds: Long, val scrollDepthPercent: Int) : ArticleDetailEvent
    data class LogRelatedClick(val relatedBackendId: String) : ArticleDetailEvent
    data object ToggleSaved : ArticleDetailEvent
    data object SaveOnly : ArticleDetailEvent
    data object SnackbarConsumed : ArticleDetailEvent
}

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resolveArticleUseCase: ResolveArticleUseCase,
    private val checkArticleSavedUseCase: CheckArticleSavedUseCase,
    private val searchNewsUseCase: SearchNewsUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val saveArticleUseCase: SaveArticleUseCase,
    private val appTelemetry: AppTelemetry
) : ViewModel() {

    private val encodedUrl: String = savedStateHandle.get<String>("url").orEmpty()
    val decodedUrl: String = decodeNavUrl(encodedUrl)

    private val _internalState = MutableStateFlow(ArticleDetailUiState())

    val state: StateFlow<ArticleDetailUiState> = kotlinx.coroutines.flow.combine(
        _internalState,
        flow {
            emit(resolveArticleUseCase(decodedUrl))
        },
        flow {
            val initialSaved = checkArticleSavedUseCase(decodedUrl)
            _isSavedMutable.value = initialSaved
            _isSavedMutable.collect { if (it != null) emit(it) }
        }
    ) { internal, article, isSaved ->
        internal.copy(
            article = article,
            isSaved = isSaved
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArticleDetailUiState())

    private val _isSavedMutable = MutableStateFlow<Boolean?>(null)

    // Staff Engineer: Opposing Views / Filter Bubble Bursting
    // We dynamically extract keywords from the article title and trigger an 'everything' query
    // sorting by relevancy to fetch alternative perspectives from other publishers.
    @OptIn(ExperimentalCoroutinesApi::class)
    val relatedPerspectives: kotlinx.coroutines.flow.Flow<PagingData<Article>> = state.map { it.article }
        .filterNotNull()
        .distinctUntilChangedBy { it.url }
        .flatMapLatest { currentArticle ->
            val keywords = extractCoreKeywords(currentArticle.title)
            // Exclude the current story so it never appears among its own
            // alternative perspectives (server-side excludeId + title dedup).
            searchNewsUseCase(query = keywords, excludeId = currentArticle.backendId)
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

    fun onEvent(event: ArticleDetailEvent) {
        when (event) {
            is ArticleDetailEvent.LogReadDeep -> logReadDeep(event.durationSeconds, event.scrollDepthPercent)
            is ArticleDetailEvent.LogRelatedClick -> logRelatedClick(event.relatedBackendId)
            is ArticleDetailEvent.ToggleSaved -> toggleSaved()
            is ArticleDetailEvent.SaveOnly -> saveOnly()
            is ArticleDetailEvent.SnackbarConsumed -> _internalState.value = _internalState.value.copy(snackbarMessage = null)
        }
    }

    private fun logReadDeep(durationSeconds: Long, scrollDepthPercent: Int) {
        state.value.article?.backendId?.let { id ->
            appTelemetry.trackReadDeep(id, durationSeconds, scrollDepthPercent)
        }
    }

    private fun logRelatedClick(relatedBackendId: String) {
        appTelemetry.trackClick(relatedBackendId)
    }

    private fun toggleSaved() {
        if (_isSavedMutable.value == null) return
        viewModelScope.launch {
            val item = state.value.article
            if (item == null) {
                _internalState.value = _internalState.value.copy(snackbarMessage = "Unable to resolve this article.")
                return@launch
            }

            if (_isSavedMutable.value == true) {
                deleteArticleUseCase(item)
                _isSavedMutable.value = false
                _internalState.value = _internalState.value.copy(snackbarMessage = "Removed from saved")
            } else {
                saveArticleUseCase(item)
                _isSavedMutable.value = true
                _internalState.value = _internalState.value.copy(snackbarMessage = "Article saved")
            }
        }
    }

    private fun saveOnly() {
        if (_isSavedMutable.value == null) return
        viewModelScope.launch {
            val item = state.value.article
            if (item == null) {
                _internalState.value = _internalState.value.copy(snackbarMessage = "Unable to resolve this article.")
                return@launch
            }

            if (_isSavedMutable.value == true) {
                _internalState.value = _internalState.value.copy(snackbarMessage = "Already saved")
            } else {
                saveArticleUseCase(item)
                _isSavedMutable.value = true
                _internalState.value = _internalState.value.copy(snackbarMessage = "Article saved")
            }
        }
    }
}
