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
import androidx.compose.ui.draw.clip
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
import com.example.newsapp.ui.tokens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(navController: NavController) {
    val vm: ArticleDetailViewModel = hiltViewModel()
    val article by vm.article.collectAsState()
    val isSaved by vm.isSaved.collectAsState()
    val aiState by vm.aiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val snackbar = LocalPulseSnackbar.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.events.collect {
            val msg = when (val e = it) {
                is UiEvent.AlreadySaved, is UiEvent.Saved, is UiEvent.DeleteFailed,
                is UiEvent.NetworkError, is UiEvent.Generic -> e.message
            }
            snackbar.showSnackbar(msg)
        }
    }

    NewsBackground(Modifier.fillMaxSize()) {
        val item = article ?: return@NewsBackground EmptyState(
            title = "Article unavailable.",
            body = "We couldn’t load a preview. Open it in your browser to read the full piece.",
            actionText = "Open Reader",
            onAction = { navController.navigateToWebPage(vm.decodedUrl) },
        )

        val progress = if (scrollState.maxValue == 0) 0f
            else scrollState.value.toFloat() / scrollState.maxValue
        val sourceLabel = listOfNotNull(item.source.name, item.taxonomy?.firstOrNull()).joinToString(" · ")

        Scaffold(
            topBar = {
                Column {
                    ReaderTopBar(
                        sourceLabel = sourceLabel,
                        isSaved = isSaved,
                        onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); navController.navigateUp() },
                        onToggleSave = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); vm.toggleSaved() },
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigateToWebPage(item.url)
                        },
                        modifier = Modifier.fillMaxWidth().padding(NewsSpacing.lg),
                        shape = RoundedCornerShape(NewsRadius.md),
                    ) { Text("Read full article \u2192", style = MaterialTheme.typography.labelLarge) }
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
                    Text(item.source.name.uppercase(), style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.provenance?.status == VerificationStatus.SOURCE_VERIFIED) {
                        Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary))
                        Text("VERIFIED", style = MetaMono, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(NewsSpacing.xs))
                Text(formatDate(item.publishedAt).uppercase(),
                    style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))

                Spacer(Modifier.height(NewsSpacing.md))
                Text(item.title, style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)

                if (!item.description.isNullOrBlank()) {
                    Spacer(Modifier.height(NewsSpacing.md))
                    Text(item.description!!, style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(NewsSpacing.lg))
                AiSummaryCard(aiState = aiState)

                if (!item.content.isNullOrBlank()) {
                    Spacer(Modifier.height(NewsSpacing.lg))
                    Text(item.content!!, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }

                val related = vm.relatedPerspectives.collectAsLazyPagingItems()
                if (related.itemCount > 0) {
                    Spacer(Modifier.height(NewsSpacing.xl))
                    Text("Related perspectives", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(NewsSpacing.md))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                        contentPadding = PaddingValues(end = NewsSpacing.lg),
                    ) {
                        items(count = related.itemCount, key = { related.peek(it)?.url ?: it }) { i ->
                            related[i]?.let { r ->
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navController.navigateToArticleDetail(r.url ?: "")
                                    },
                                    modifier = Modifier.width(240.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(NewsRadius.md),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Column(Modifier.padding(NewsSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(r.source.name.orEmpty().uppercase(),
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