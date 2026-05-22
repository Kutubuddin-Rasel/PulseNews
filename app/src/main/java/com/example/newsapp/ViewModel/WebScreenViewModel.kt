package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.decodeNavUrl
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.domain.util.OfflineHtmlCache
import com.example.newsapp.domain.util.ParsedArticle
import com.example.newsapp.domain.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

sealed class ReaderState {
    object Loading : ReaderState()
    data class Success(val article: ParsedArticle) : ReaderState()
    data class Error(val message: String) : ReaderState()
}

@HiltViewModel
class WebScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository,
    private val savedArticleRepository: SavedArticleRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val offlineHtmlCache: OfflineHtmlCache
) : ViewModel() {

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

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val okHttpClient = okhttp3.OkHttpClient()

    val readerState: StateFlow<ReaderState> = flow {
        emit(ReaderState.Loading)
        val online = connectivityMonitor.isOnline()
        var htmlToParse: String? = null
        if (online) {
            try {
                val document = org.jsoup.Jsoup.connect(decodedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(10000)
                    .get()
                htmlToParse = document.outerHtml()
            } catch (e: Exception) {
                htmlToParse = offlineHtmlCache.getCachedHtml(decodedUrl)
            }
        } else {
            htmlToParse = offlineHtmlCache.getCachedHtml(decodedUrl)
        }
        
        if (htmlToParse != null) {
            emit(ReaderState.Success(HtmlParser.parse(htmlToParse)))
        } else {
            emit(ReaderState.Error("Failed to fetch article content."))
        }
    }.flowOn(Dispatchers.IO)
     .stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = ReaderState.Loading
     )

    init {
        _isOnline.value = connectivityMonitor.isOnline()
    }

    fun saveCurrentArticle() {
        val candidate = article.value
        if (candidate == null) {
            viewModelScope.launch { _events.emit(UiEvent.Generic("Article details are not available.")) }
            return
        }

        viewModelScope.launch {
            if (savedArticleRepository.isSaved(candidate.url)) {
                _events.emit(UiEvent.AlreadySaved())
            } else {
                savedArticleRepository.saveArticle(candidate)
                _isSavedMutable.value = true
                _events.emit(UiEvent.Saved())
            }
        }
    }

    fun toggleSaved() {
        val candidate = article.value
        if (candidate == null) {
            viewModelScope.launch { _events.emit(UiEvent.Generic("Article details are not available.")) }
            return
        }

        viewModelScope.launch {
            if (savedArticleRepository.isSaved(candidate.url)) {
                savedArticleRepository.deleteArticle(candidate)
                _isSavedMutable.value = false
                _events.emit(UiEvent.Generic("Removed from saved"))
            } else {
                savedArticleRepository.saveArticle(candidate)
                _isSavedMutable.value = true
                _events.emit(UiEvent.Saved())
            }
        }
    }
}
