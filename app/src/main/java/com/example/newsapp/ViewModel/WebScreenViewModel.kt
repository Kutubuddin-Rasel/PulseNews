package com.example.newsapp.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.decodeNavUrl
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.domain.usecase.news.ResolveArticleUseCase
import com.example.newsapp.domain.usecase.saved.CheckArticleSavedUseCase
import com.example.newsapp.domain.usecase.saved.DeleteArticleUseCase
import com.example.newsapp.domain.usecase.saved.SaveArticleUseCase
import com.example.newsapp.module.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.domain.util.OfflineHtmlCache
import com.example.newsapp.domain.util.ParsedArticle
import com.example.newsapp.domain.util.HtmlParser
import com.example.newsapp.domain.util.tts.TtsEngine
import com.example.newsapp.domain.tracker.EngagementTracker
import com.example.newsapp.domain.util.AiSummarizer
import com.example.newsapp.domain.util.AiSummaryResult
import com.example.newsapp.data.util.nlp.LocalSummarizer
import com.example.newsapp.domain.util.ArticleBlock
import com.example.newsapp.domain.util.AppTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn

sealed interface ReaderState {
    data object Loading : ReaderState
    data class Success(val article: ParsedArticle) : ReaderState
    data class Error(val message: String) : ReaderState
}

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Success(val summary: String) : AiState
    data class SuccessFallback(val summary: String) : AiState
    data class Error(val message: String) : AiState
}

sealed interface AudioState {
    data object Idle : AudioState
    data object Synthesizing : AudioState
    data class Ready(val uri: android.net.Uri) : AudioState
    data class Error(val message: String) : AudioState
}

@HiltViewModel
class WebScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resolveArticleUseCase: ResolveArticleUseCase,
    private val checkArticleSavedUseCase: CheckArticleSavedUseCase,
    private val saveArticleUseCase: SaveArticleUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val offlineHtmlCache: OfflineHtmlCache,
    private val aiSummarizer: AiSummarizer,
    private val localSummarizer: LocalSummarizer,
    private val ttsEngine: TtsEngine,
    private val engagementTracker: EngagementTracker,
    private val appTelemetry: AppTelemetry
) : ViewModel() {

    private val encodedUrl: String = savedStateHandle.get<String>("url").orEmpty()
    val decodedUrl: String = decodeNavUrl(encodedUrl)

    val article: StateFlow<Article?> = flow {
        emit(resolveArticleUseCase(decodedUrl))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _isSavedMutable = MutableStateFlow<Boolean?>(null)
    
    // To prevent double counting
    private var hasRecordedReadForThisArticle = false

    val isSaved: StateFlow<Boolean> = flow {
        val initialSaved = checkArticleSavedUseCase(decodedUrl)
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

    val readerState: StateFlow<ReaderState> = flow {
        emit(ReaderState.Loading)
        // W5: serve cache-first. Reader HTML per URL is effectively immutable, so a previously
        // read article opens instantly and works offline instead of re-scraping the publisher on
        // every open (heavy data + latency + anti-scraping fragility).
        var htmlToParse: String? = offlineHtmlCache.getCachedHtml(decodedUrl)
        if (htmlToParse == null && connectivityMonitor.isOnline()) {
            try {
                val document = org.jsoup.Jsoup.connect(decodedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(10000)
                    .get()
                htmlToParse = document.outerHtml()
                // Persist what we just fetched so subsequent opens are cache-first / offline-capable.
                htmlToParse?.let { offlineHtmlCache.cacheHtml(decodedUrl, it) }
            } catch (e: Exception) {
                htmlToParse = null
            }
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

    private val _aiSummaryState = MutableStateFlow<AiState>(AiState.Idle)
    val aiSummaryState: StateFlow<AiState> = _aiSummaryState

    private val _audioState = MutableStateFlow<AudioState>(AudioState.Idle)
    val audioState: StateFlow<AudioState> = _audioState

    init {
        _isOnline.value = connectivityMonitor.isOnline()

        // W4: do NOT auto-call the costed/rate-limited backend summary on every open. If the feed
        // already carried a globally-cached summary (CONF2), surface it for free; otherwise stay
        // Idle and wait for an explicit user "Summarize" action via requestSummary().
        viewModelScope.launch {
            readerState.collect { state ->
                if (state is ReaderState.Success && _aiSummaryState.value == AiState.Idle) {
                    val feedSummary = article.value?.summary?.takeIf { it.isNotBlank() }
                    if (feedSummary != null) {
                        _aiSummaryState.value = AiState.Success(feedSummary)
                    }
                }
            }
        }
    }

    /**
     * W4: lazy, user-triggered summary. No-op if a summary is already shown or in flight, so the
     * result is effectively cached for the article's reader session. Calls the costed backend
     * endpoint only on explicit request; on rate-limit, falls back to on-device extractive NLP
     * run off the main thread (W1 — TextRank is O(n²) over up to 1500 words).
     */
    fun requestSummary() {
        when (_aiSummaryState.value) {
            is AiState.Loading, is AiState.Success, is AiState.SuccessFallback -> return
            else -> { /* Idle or Error: proceed */ }
        }

        val state = readerState.value
        if (state !is ReaderState.Success) {
            _aiSummaryState.value = AiState.Error("Article content is still loading.")
            return
        }

        _aiSummaryState.value = AiState.Loading
        viewModelScope.launch {
            // Take up to 1500 words to stay within safe token limits and maintain speed.
            val fullText = state.article.blocks
                .filterIsInstance<ArticleBlock.Text>()
                .joinToString("\n\n") { it.content }
            val truncatedText = fullText.split("\\s+".toRegex()).take(1500).joinToString(" ")

            val backendId = article.value?.backendId
            if (backendId.isNullOrEmpty()) {
                _aiSummaryState.value = AiState.Error("Article ID not found.")
                return@launch
            }

            when (val result = aiSummarizer.generateTlDr(backendId, truncatedText)) {
                is AiSummaryResult.Success -> {
                    Log.d("WebScreenVM", "AI Summary Success for $backendId")
                    _aiSummaryState.value = AiState.Success(result.summary)
                }
                is AiSummaryResult.RateLimitExceeded -> {
                    Log.w("WebScreenVM", "AI Summary RateLimited for $backendId. Extractive NLP fallback.")
                    val fallbackSummary = withContext(Dispatchers.Default) {
                        localSummarizer.summarize(truncatedText, state.article.title)
                    }
                    _aiSummaryState.value = AiState.SuccessFallback(fallbackSummary)
                }
                is AiSummaryResult.Error -> {
                    Log.e("WebScreenVM", "AI Summary Error for $backendId: ${result.message}")
                    _aiSummaryState.value = if (result.message == "GENERATION_FAILED") {
                        AiState.Error("Unable to generate an AI summary for this article.")
                    } else {
                        AiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun saveCurrentArticle() {
        val candidate = article.value
        if (candidate == null) {
            viewModelScope.launch { _events.emit(UiEvent.Generic("Article details are not available.")) }
            return
        }

        viewModelScope.launch {
            if (checkArticleSavedUseCase(candidate.url)) {
                _events.emit(UiEvent.AlreadySaved())
            } else {
                saveArticleUseCase(candidate)
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
            if (checkArticleSavedUseCase(candidate.url)) {
                deleteArticleUseCase(candidate)
                _isSavedMutable.value = false
                _events.emit(UiEvent.Generic("Removed from saved"))
            } else {
                saveArticleUseCase(candidate)
                _isSavedMutable.value = true
                _events.emit(UiEvent.Saved())
            }
        }
    }

    fun startAudioNarration() {
        val currentState = readerState.value
        if (currentState !is ReaderState.Success) return

        if (_audioState.value is AudioState.Synthesizing || _audioState.value is AudioState.Ready) return

        _audioState.value = AudioState.Synthesizing

        viewModelScope.launch {
            try {
                // Combine title and all text blocks into one text block for TTS
                val fullText = buildString {
                    appendLine(currentState.article.title)
                    currentState.article.blocks
                        .filterIsInstance<ArticleBlock.Text>()
                        .forEach { appendLine(it.content) }
                }
                
                // Use a simplified version of the URL as a unique ID for caching
                val articleId = decodedUrl.hashCode().toString()
                
                val uri = ttsEngine.synthesizeToUri(fullText, articleId)
                _audioState.value = AudioState.Ready(uri)
            } catch (e: Exception) {
                _audioState.value = AudioState.Error(e.message ?: "Failed to generate audio.")
            }
        }
    }

    fun recordArticleRead() {
        if (!hasRecordedReadForThisArticle) {
            hasRecordedReadForThisArticle = true
            viewModelScope.launch {
                // EG2: attribute the read to the article's real primary category (from taxonomy) so
                // engagement isn't all bucketed under "general"; fall back only when truly unknown.
                val category = article.value?.taxonomy?.categories?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: "general"
                engagementTracker.recordArticleRead(category)
            }
        }
    }

    private val enterTimeMs = System.currentTimeMillis()

    fun recordDwell(scrollDepthPercent: Int) {
        val durationSeconds = (System.currentTimeMillis() - enterTimeMs) / 1000
        val backendId = article.value?.backendId ?: decodedUrl
        appTelemetry.trackReadDeep(backendId, durationSeconds, scrollDepthPercent)
    }
}
