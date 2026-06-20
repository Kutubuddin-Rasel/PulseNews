package com.example.newsapp.Screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.newsapp.ViewModel.WebScreenViewModel
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.domain.util.reader.ReaderMode
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.components.ReaderErrorPanel
import com.example.newsapp.ui.components.ReaderProgressStrip
import com.example.newsapp.ui.components.ReaderTopBar
import com.example.newsapp.ui.components.AudioPlaybackController
import com.example.newsapp.ui.components.LocalPulseSnackbar
import com.example.newsapp.ui.components.reader.ReaderWebView
import com.example.newsapp.ui.components.reader.ReadingSettingsSheet
import com.example.newsapp.ui.components.reader.buildBionicString
import com.example.newsapp.ui.theme.readerColorsFor
import com.example.newsapp.ui.tokens.NewsSpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch

@Composable
fun WebScreen(navController: NavController) {
    val viewModel: WebScreenViewModel = hiltViewModel()
    val context = LocalContext.current
    val snackbar = LocalPulseSnackbar.current
    val scope = rememberCoroutineScope()
    val article by viewModel.article.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val readerState by viewModel.readerState.collectAsState()
    val aiSummaryState by viewModel.aiSummaryState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val prefs by viewModel.readingPreferences.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is UiEvent.AlreadySaved -> event.message
                is UiEvent.Saved -> event.message
                is UiEvent.DeleteFailed -> event.message
                is UiEvent.NetworkError -> event.message
                is UiEvent.Generic -> event.message
            }
            snackbar.showSnackbar(message)
        }
    }

    val targetUrl = viewModel.decodedUrl
    val safeUrl = targetUrl.takeIf(::isSafeUrl)

    NewsBackground(modifier = Modifier.fillMaxSize()) {
        if (safeUrl == null) {
            ReaderErrorPanel(
                message = "Malformed or unsafe URL.",
                onRetry = { navController.popBackStack() },
                onOpenExternal = { navController.popBackStack() }
            )
            return@NewsBackground
        }

        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val isHeaderVisible by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex == 0
            }
        }

        var maxScrollPercent by remember { mutableIntStateOf(0) }
        var webLoadFailed by remember { mutableStateOf(false) }

        // Live reading progress (fraction of items scrolled past) drives the strip + read-time chip.
        val progress by remember {
            derivedStateOf {
                val li = listState.layoutInfo
                val total = li.totalItemsCount
                if (total == 0) 0f else ((li.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1f) / total
            }
        }
        // Focus mode: the visible item whose center is closest to the viewport center is "active";
        // every other block is dimmed. Absolute list index (header is 0, block[i] is i+1).
        val activeIndex by remember {
            derivedStateOf {
                val li = listState.layoutInfo
                if (li.visibleItemsInfo.isEmpty()) -1
                else {
                    val center = (li.viewportStartOffset + li.viewportEndOffset) / 2
                    li.visibleItemsInfo.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - center) }!!.index
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.recordDwell(maxScrollPercent)
            }
        }

        // 50% Scroll Detection for Gamification
        LaunchedEffect(listState) {
            androidx.compose.runtime.snapshotFlow { listState.layoutInfo }
                .collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    if (totalItems > 0) {
                        val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val currentPercent = ((lastVisibleItemIndex + 1) * 100) / totalItems
                        if (currentPercent > maxScrollPercent) {
                            maxScrollPercent = currentPercent
                        }
                        // If user scrolled past 50% of the content
                        if (lastVisibleItemIndex >= totalItems / 2) {
                            viewModel.recordArticleRead()
                        }
                    }
                }
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isHeaderVisible,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    ReaderTopBar(
                        sourceLabel = article?.source?.name ?: "Reader",
                        isSaved = isSaved,
                        readerMode = mode,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { showSettings = true },
                        onToggleMode = viewModel::toggleMode,
                        onToggleSave = viewModel::toggleSaved,
                        onShare = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, safeUrl)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share article"))
                            } catch (e: Exception) {
                                scope.launch { snackbar.showSnackbar("Unable to share") }
                            }
                        },
                        onOpenExternal = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                            } catch (e: Exception) {
                                scope.launch { snackbar.showSnackbar("Unable to open browser") }
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                // Audio narration is a Reader-mode affordance (it reads the extracted blocks).
                if (mode == ReaderMode.Reader &&
                    readerState is com.example.newsapp.ViewModel.ReaderState.Success &&
                    audioState !is com.example.newsapp.ViewModel.AudioState.Ready) {
                    FloatingActionButton(
                        onClick = { viewModel.startAudioNarration() },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        if (audioState is com.example.newsapp.ViewModel.AudioState.Synthesizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Audiotrack, contentDescription = "Listen to Article")
                        }
                    }
                }
            },
            bottomBar = {
                if (audioState is com.example.newsapp.ViewModel.AudioState.Ready) {
                    val uri = (audioState as com.example.newsapp.ViewModel.AudioState.Ready).uri
                    val title = (readerState as? com.example.newsapp.ViewModel.ReaderState.Success)?.content?.title ?: "Article"
                    AudioPlaybackController(
                        uri = uri,
                        title = title,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                when (val state = readerState) {
                    is com.example.newsapp.ViewModel.ReaderState.Loading -> {
                        ReaderProgressStrip(progress = 0f, isLoading = true)
                    }
                    is com.example.newsapp.ViewModel.ReaderState.Error -> {
                        ReaderProgressStrip(progress = 0f, isLoading = false)
                        ReaderErrorPanel(
                            message = state.message,
                            onRetry = { /* Reload logic can be added if needed */ },
                            onOpenExternal = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                                } catch (e: Exception) {
                                    scope.launch { snackbar.showSnackbar("Unable to open browser") }
                                }
                            }
                        )
                    }
                    is com.example.newsapp.ViewModel.ReaderState.Success -> {
                        val rc = readerColorsFor(prefs.theme)
                        when (mode) {
                            ReaderMode.Web -> {
                                if (webLoadFailed) {
                                    ReaderErrorPanel(
                                        message = "This page couldn't be loaded.",
                                        onRetry = { webLoadFailed = false },
                                        onOpenExternal = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                                            } catch (e: Exception) {
                                                scope.launch { snackbar.showSnackbar("Unable to open browser") }
                                            }
                                        }
                                    )
                                } else {
                                    ReaderWebView(
                                        url = safeUrl,
                                        onError = { webLoadFailed = true },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            ReaderMode.Reader -> {
                                ReaderProgressStrip(progress = progress, isLoading = false)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(rc.background),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .widthIn(max = prefs.measureWidth.maxContentWidthDp.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = NewsSpacing.lg),
                                        contentPadding = PaddingValues(bottom = 88.dp)
                                    ) {
                                        item {
                                            if (state.content.heroImageUrl != null) {
                                                coil.compose.AsyncImage(
                                                    model = state.content.heroImageUrl,
                                                    contentDescription = "${state.content.title} hero image",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(220.dp)
                                                        .clip(MaterialTheme.shapes.large),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(NewsSpacing.md))
                                            }
                                            Text(
                                                text = state.content.title,
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = rc.text
                                            )
                                            Spacer(modifier = Modifier.height(NewsSpacing.xs))
                                            Text(
                                                text = "${state.content.estReadMinutes} min read · ${state.content.minutesLeft(progress)} min left",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = rc.secondaryText
                                            )
                                            Spacer(modifier = Modifier.height(NewsSpacing.lg))
                                            com.example.newsapp.ui.components.AiSummaryCard(
                                                aiState = aiSummaryState,
                                                onSummarizeClick = { viewModel.requestSummary() }
                                            )
                                            Spacer(modifier = Modifier.height(NewsSpacing.lg))
                                        }
                                        // LST1: key on the (immutable, never-reordered) block index and tag
                                        // each block by type so the lazy list recycles compositions only
                                        // among like-typed blocks (Text↔Text, Image↔Image).
                                        itemsIndexed(
                                            items = state.content.blocks,
                                            key = { index, _ -> index },
                                            contentType = { _, block ->
                                                when (block) {
                                                    is com.example.newsapp.domain.util.ArticleBlock.Text -> "text"
                                                    is com.example.newsapp.domain.util.ArticleBlock.Image -> "image"
                                                    is com.example.newsapp.domain.util.ArticleBlock.Video -> "video"
                                                }
                                            }
                                        ) { index, block ->
                                            val dim = prefs.focusEnabled && (index + 1) != activeIndex
                                            val blockAlpha = if (dim) 0.4f else 1f
                                            when (block) {
                                                is com.example.newsapp.domain.util.ArticleBlock.Text -> {
                                                    val base = when (block.type) {
                                                        com.example.newsapp.domain.util.TextType.H1 -> MaterialTheme.typography.headlineMedium
                                                        com.example.newsapp.domain.util.TextType.H2 -> MaterialTheme.typography.titleLarge
                                                        com.example.newsapp.domain.util.TextType.H3 -> MaterialTheme.typography.titleMedium
                                                        com.example.newsapp.domain.util.TextType.PARAGRAPH -> MaterialTheme.typography.bodyLarge
                                                    }
                                                    val style = base.copy(
                                                        fontSize = base.fontSize * prefs.fontScale,
                                                        lineHeight = base.fontSize * prefs.fontScale * prefs.lineHeight.multiplier,
                                                        color = if (block.type == com.example.newsapp.domain.util.TextType.PARAGRAPH) rc.secondaryText else rc.text
                                                    )
                                                    if (prefs.bionicEnabled && block.type == com.example.newsapp.domain.util.TextType.PARAGRAPH) {
                                                        Text(
                                                            text = buildBionicString(block.content),
                                                            style = style,
                                                            modifier = Modifier.alpha(blockAlpha)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = block.content,
                                                            style = style,
                                                            modifier = Modifier.alpha(blockAlpha)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(NewsSpacing.md))
                                                }
                                                is com.example.newsapp.domain.util.ArticleBlock.Image -> {
                                                    coil.compose.AsyncImage(
                                                        model = block.url,
                                                        contentDescription = block.caption,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(16f / 9f)
                                                            .clip(MaterialTheme.shapes.medium)
                                                            .alpha(blockAlpha),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                    if (block.caption != null) {
                                                        Spacer(modifier = Modifier.height(NewsSpacing.xs))
                                                        Text(
                                                            text = block.caption,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = rc.secondaryText,
                                                            modifier = Modifier.alpha(blockAlpha)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(NewsSpacing.lg))
                                                }
                                                is com.example.newsapp.domain.util.ArticleBlock.Video -> {
                                                    // A simple placeholder for video support. In a production app,
                                                    // this would use ExoPlayer or an isolated WebView iframe.
                                                    androidx.compose.material3.Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                            .alpha(blockAlpha),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = MaterialTheme.shapes.medium
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Icon(
                                                                    Icons.Default.PlayArrow,
                                                                    contentDescription = "Play Video",
                                                                    modifier = Modifier.size(48.dp),
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Text("${block.platform} Video", style = MaterialTheme.typography.labelLarge)
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(NewsSpacing.lg))
                                                }
                                            }
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(NewsSpacing.lg))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            ReadingSettingsSheet(
                prefs = prefs,
                onFontScale = viewModel::setFontScale,
                onLineHeight = viewModel::setLineHeight,
                onWidth = viewModel::setMeasureWidth,
                onTheme = viewModel::setTheme,
                onBionic = viewModel::setBionicEnabled,
                onFocus = viewModel::setFocusEnabled,
                onDismiss = { showSettings = false }
            )
        }
    }
}

private fun isSafeUrl(url: String): Boolean {
    val lower = url.lowercase()
    val parsedScheme = runCatching { Uri.parse(url).scheme.orEmpty() }.getOrDefault("")
    return (lower.startsWith("https://") || lower.startsWith("http://")) &&
        parsedScheme in setOf("http", "https") &&
        !lower.startsWith("javascript:")
}
