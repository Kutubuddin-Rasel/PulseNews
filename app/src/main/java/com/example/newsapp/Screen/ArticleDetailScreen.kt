package com.example.newsapp.Screen

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.newsapp.ViewModel.ArticleDetailViewModel
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.domain.model.VerificationStatus
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.navigateToWebPage
import com.example.newsapp.ui.components.*
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.theme.ReaderBody
import com.example.newsapp.ui.theme.ReaderLead
import com.example.newsapp.ui.tokens.*

import com.example.newsapp.ViewModel.ArticleDetailUiState
import com.example.newsapp.ViewModel.ArticleDetailEvent
import androidx.paging.PagingData
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow

@Composable
fun ArticleDetailRoute(navController: NavController) {
    val vm: ArticleDetailViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    
    val relatedPerspectives = vm.relatedPerspectives.collectAsLazyPagingItems()

    ArticleDetailScreen(
        state = state,
        onEvent = vm::onEvent,
        decodedUrl = vm.decodedUrl,
        relatedPerspectives = relatedPerspectives,
        onNavigateUp = { navController.navigateUp() },
        onNavigateToWebPage = { url -> navController.navigateToWebPage(url) },
        onNavigateToArticleDetail = { url -> navController.navigateToArticleDetail(url) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    state: ArticleDetailUiState,
    onEvent: (ArticleDetailEvent) -> Unit,
    decodedUrl: String,
    relatedPerspectives: androidx.paging.compose.LazyPagingItems<Article>,
    onNavigateUp: () -> Unit,
    onNavigateToWebPage: (String) -> Unit,
    onNavigateToArticleDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val snackbar = LocalPulseSnackbar.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbar.showSnackbar(msg)
            onEvent(ArticleDetailEvent.SnackbarConsumed)
        }
    }

    DisposableEffect(Unit) {
        val startTime = System.currentTimeMillis()
        onDispose {
            val endTime = System.currentTimeMillis()
            val durationSeconds = (endTime - startTime) / 1000L
            val scrollDepthPercent = if (scrollState.maxValue == 0) {
                0
            } else {
                ((scrollState.value.toFloat() / scrollState.maxValue.toFloat()) * 100).toInt()
            }
            onEvent(ArticleDetailEvent.LogReadDeep(durationSeconds, scrollDepthPercent))
        }
    }

    NewsBackground(Modifier.fillMaxSize()) {
        val item = state.article ?: return@NewsBackground EmptyState(
            title = "Article unavailable.",
            body = "We couldn’t load a preview. Open it in your browser to read the full piece.",
            actionText = "Open Reader",
            onAction = { onNavigateToWebPage(decodedUrl) },
        )

        val progress = if (scrollState.maxValue == 0) 0f
            else scrollState.value.toFloat() / scrollState.maxValue
        val sourceLabel = listOfNotNull(item.source?.name, item.taxonomy?.categories?.firstOrNull()).joinToString(" · ")

        Scaffold(
            topBar = {
                Column {
                    ReaderTopBar(
                        sourceLabel = sourceLabel,
                        isSaved = state.isSaved,
                        onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateUp() },
                        onToggleSave = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onEvent(ArticleDetailEvent.ToggleSaved) },
                        onShare = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, item.url ?: "")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        },
                        onOpenExternal = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                            }.onFailure { scope.launch { snackbar.showSnackbar("Unable to open browser") } }
                        },
                    )
                    ReaderProgressStrip(progress = progress)
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(top = NewsSpacing.xl)
                        .padding(horizontal = NewsSpacing.lg)
                        .padding(bottom = NewsSpacing.lg)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            item.url?.let { onNavigateToWebPage(it) }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(NewsRadius.pill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Read full article",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(Modifier.width(NewsSpacing.sm))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            containerColor = Color.Transparent,
        ) { padding ->
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(padding)
                    .padding(horizontal = NewsSpacing.lg)
            ) {
                AsyncImage(
                    model = item.urlToImage,
                    contentDescription = "${item.title} hero",
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                        .clip(RoundedCornerShape(NewsRadius.md))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                Spacer(Modifier.height(NewsSpacing.lg))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.source?.name.orEmpty().uppercase(), style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.provenance?.status == VerificationStatus.SOURCE_VERIFIED) {
                        Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary))
                        Text("VERIFIED", style = MetaMono, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(NewsSpacing.xs))
                Text(formatDate(item.publishedAt ?: "").uppercase(),
                    style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))

                Spacer(Modifier.height(NewsSpacing.md))
                Text(item.title ?: "", style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)

                if (!item.description.isNullOrBlank()) {
                    Spacer(Modifier.height(NewsSpacing.md))
                    Text(
                        text = item.description,
                        style = ReaderLead,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // This screen never generates a summary (aiState stays Idle), so the card only
                // renders if one is ever supplied — avoids showing a dead "Summarize" action here.
                if (state.aiState !is com.example.newsapp.ViewModel.AiState.Idle) {
                    Spacer(Modifier.height(NewsSpacing.lg))
                    AiSummaryCard(aiState = state.aiState, onSummarizeClick = {})
                }

                if (!item.content.isNullOrBlank()) {
                    Spacer(Modifier.height(NewsSpacing.lg))
                    Text(
                        text = item.content,
                        style = ReaderBody,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (relatedPerspectives.itemCount > 0) {
                    Spacer(Modifier.height(NewsSpacing.xl))
                    Text("Related perspectives", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(NewsSpacing.md))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                        contentPadding = PaddingValues(end = NewsSpacing.lg),
                    ) {
                        items(count = relatedPerspectives.itemCount, key = { relatedPerspectives.peek(it)?.url ?: it }) { i ->
                            relatedPerspectives[i]?.let { r ->
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onEvent(ArticleDetailEvent.LogRelatedClick(r.backendId ?: r.url ?: ""))
                                        onNavigateToArticleDetail(r.url ?: "")
                                    },
                                    modifier = Modifier.width(240.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(NewsRadius.md),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Column(Modifier.padding(NewsSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(r.source?.name.orEmpty().uppercase(),
                                            style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(r.title.orEmpty(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(NewsSpacing.xxl))
            }
        }
    }
}