package com.example.newsapp.Screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CheckCircle
import com.example.newsapp.ViewModel.ArticleDetailViewModel
import com.example.newsapp.domain.model.UiEvent
import com.example.newsapp.navigateToWebPage
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.EmptyState
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.components.enterpriseTopBarSpacing
import com.example.newsapp.ui.components.formatDate
import com.example.newsapp.ui.tokens.NewsSpacing

@Composable
fun ArticleDetailScreen(navController: NavController) {
    val viewModel: ArticleDetailViewModel = hiltViewModel()
    val article by viewModel.article.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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

    NewsBackground(modifier = Modifier.fillMaxSize()) {
        val item = article
        if (item == null) {
            EmptyState(
                message = "Unable to load article preview.",
                actionText = "Open Reader",
                onAction = { navController.navigateToWebPage(viewModel.decodedUrl) }
            )
            return@NewsBackground
        }

        var showMenu by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigateUp()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleSaved()
                        }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (isSaved) "Unsave" else "Save"
                            )
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showMenu = true
                        }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open in browser") },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showMenu = false
                                    runCatching {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                        context.startActivity(browserIntent)
                                    }.onFailure {
                                        Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NewsSpacing.lg),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.logInteraction("read_full_article")
                        navController.navigateToWebPage(item.url)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text("Read full article", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = NewsSpacing.lg)
            ) {
                if (!item.urlToImage.isNullOrBlank()) {
                    AsyncImage(
                        model = item.urlToImage,
                        contentDescription = "${item.title} hero image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(modifier = Modifier.height(NewsSpacing.md))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.source.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (item.provenance?.status == com.example.newsapp.domain.model.VerificationStatus.SOURCE_VERIFIED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                            contentDescription = "Verified Publisher",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified via ${item.provenance.verificationMethod} from ${item.provenance.trustedSigner}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = formatDate(item.publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(NewsSpacing.sm))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NewsSpacing.sm))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!item.content.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NewsSpacing.sm))
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(NewsSpacing.xl))

                // Staff Engineer: Opposing Views UI
                val relatedArticles = viewModel.relatedPerspectives.collectAsLazyPagingItems()
                AnimatedVisibility(
                    visible = relatedArticles.itemCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Related Perspectives",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = NewsSpacing.md)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(NewsSpacing.md),
                            contentPadding = PaddingValues(end = NewsSpacing.lg)
                        ) {
                            items(
                                count = relatedArticles.itemCount,
                                key = { index -> relatedArticles.peek(index)?.url ?: index }
                            ) { index ->
                                val related = relatedArticles[index]
                                if (related != null) {
                                    Card(
                                        modifier = Modifier
                                            .width(260.dp)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.logRelatedInteraction(related.backendId, "article_clicked")
                                                navController.navigateToArticleDetail(related.url ?: "")
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(NewsSpacing.md)) {
                                            Text(
                                                text = related.source.name ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(NewsSpacing.xs))
                                            Text(
                                                text = related.title ?: "",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(NewsSpacing.xl))
            }
        }
    }
}
