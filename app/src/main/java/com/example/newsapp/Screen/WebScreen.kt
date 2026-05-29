package com.example.newsapp.Screen

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.newsapp.ViewModel.WebScreenViewModel
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.components.ReaderErrorPanel
import com.example.newsapp.ui.components.ReaderProgressStrip
import com.example.newsapp.ui.components.ReaderTopBar
import com.example.newsapp.ui.components.AudioPlaybackController
import com.example.newsapp.ui.tokens.NewsSpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment

@Composable
fun WebScreen(navController: NavController) {
    val viewModel: WebScreenViewModel = hiltViewModel()
    val context = LocalContext.current
    val article by viewModel.article.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val readerState by viewModel.readerState.collectAsState()
    val aiSummaryState by viewModel.aiSummaryState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is UiEvent.AlreadySaved -> event.message
                is UiEvent.Saved -> event.message
                is UiEvent.DeleteFailed -> event.message
                is UiEvent.NetworkError -> event.message
                is UiEvent.Generic -> event.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
            androidx.compose.runtime.derivedStateOf {
                listState.firstVisibleItemIndex == 0
            }
        }

        // 50% Scroll Detection for Gamification
        LaunchedEffect(listState) {
            androidx.compose.runtime.snapshotFlow { listState.layoutInfo }
                .collect { layoutInfo ->
                    val totalItems = layoutInfo.totalItemsCount
                    if (totalItems > 0) {
                        val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
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
                        onBack = { navController.popBackStack() },
                        onToggleSave = viewModel::toggleSaved,
                        onShare = {
                            runCatching {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, safeUrl)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share article"))
                            }.onFailure {
                                Toast.makeText(context, "Unable to share", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenExternal = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                            }.onFailure {
                                Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (readerState is com.example.newsapp.ViewModel.ReaderState.Success && audioState !is com.example.newsapp.ViewModel.AudioState.Ready) {
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
                    val title = (readerState as? com.example.newsapp.ViewModel.ReaderState.Success)?.article?.title ?: "Article"
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
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                                }.onFailure {
                                    Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    is com.example.newsapp.ViewModel.ReaderState.Success -> {
                        ReaderProgressStrip(progress = 0f, isLoading = false)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = NewsSpacing.lg),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            item {
                                if (state.article.heroImageUrl != null) {
                                coil.compose.AsyncImage(
                                        model = state.article.heroImageUrl,
                                        contentDescription = "${state.article.title} hero image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(MaterialTheme.shapes.large),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(NewsSpacing.md))
                                }
                                Text(
                                    text = state.article.title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(NewsSpacing.lg))
                                com.example.newsapp.ui.components.AiSummaryCard(aiState = aiSummaryState)
                                Spacer(modifier = Modifier.height(NewsSpacing.lg))
                            }
                            items(state.article.blocks) { block ->
                                when (block) {
                                    is com.example.newsapp.domain.util.ArticleBlock.Text -> {
                                        val style = when (block.type) {
                                            com.example.newsapp.domain.util.TextType.H1 -> MaterialTheme.typography.headlineMedium
                                            com.example.newsapp.domain.util.TextType.H2 -> MaterialTheme.typography.titleLarge
                                            com.example.newsapp.domain.util.TextType.H3 -> MaterialTheme.typography.titleMedium
                                            com.example.newsapp.domain.util.TextType.PARAGRAPH -> MaterialTheme.typography.bodyLarge
                                        }
                                        val color = when (block.type) {
                                            com.example.newsapp.domain.util.TextType.PARAGRAPH -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.onBackground
                                        }
                                        Text(
                                            text = block.content,
                                            style = style,
                                            color = color
                                        )
                                        Spacer(modifier = Modifier.height(NewsSpacing.md))
                                    }
                                    is com.example.newsapp.domain.util.ArticleBlock.Image -> {
                                        coil.compose.AsyncImage(
                                            model = block.url,
                                            contentDescription = block.caption,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.medium),
                                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                                        )
                                        if (block.caption != null) {
                                            Spacer(modifier = Modifier.height(NewsSpacing.xs))
                                            Text(
                                                text = block.caption,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                .height(200.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
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

private fun isSafeUrl(url: String): Boolean {
    val lower = url.lowercase()
    val parsedScheme = runCatching { Uri.parse(url).scheme.orEmpty() }.getOrDefault("")
    return (lower.startsWith("https://") || lower.startsWith("http://")) &&
        parsedScheme in setOf("http", "https") &&
        !lower.startsWith("javascript:")
}
