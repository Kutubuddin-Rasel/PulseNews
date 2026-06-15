package com.example.newsapp.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.newsapp.Routes
import com.example.newsapp.ViewModel.SavedEvent
import com.example.newsapp.ViewModel.SavedUiState
import com.example.newsapp.ViewModel.SavedViewModel
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.*
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@Composable
fun SavedRoute(navController: NavController) {
    val vm: SavedViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()

    SavedArticleScreen(
        uiState = uiState,
        onEvent = vm::onEvent,
        onNavigateToHome = {
            navController.navigate(Routes.home) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateToArticle = { url -> navController.navigateToArticleDetail(url) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedArticleScreen(
    uiState: SavedUiState,
    onEvent: (SavedEvent) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToArticle: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.snackbarMessage, uiState.deletedArticleForUndo) {
        if (uiState.snackbarMessage != null) {
            val articleForUndo = uiState.deletedArticleForUndo
            val result = if (articleForUndo != null) {
                snackbarHostState.showSnackbar(
                    message = uiState.snackbarMessage,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Long
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = uiState.snackbarMessage,
                    duration = SnackbarDuration.Short
                )
            }
            
            if (result == SnackbarResult.ActionPerformed && articleForUndo != null) {
                onEvent(SavedEvent.UndoDelete(articleForUndo))
            } else {
                onEvent(SavedEvent.SnackbarDismissed)
            }
        }
    }

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { SavedHeader(count = (uiState.articles as? UiState.Success)?.data?.size ?: 0) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (val s = uiState.articles) {
                    UiState.Idle, UiState.Loading -> FeedSkeleton()
                    is UiState.Error -> ErrorState(
                        title = "Couldn’t load saved.",
                        body = s.message, retryable = s.retryable, onRetry = {},
                    )
                    is UiState.Empty -> EmptyState(
                        title = "Nothing in ‘Saved’ yet.",
                        body = "Bookmark stories you want to come back to. They’ll appear here, available offline.",
                        actionText = "Browse top stories",
                        onAction = onNavigateToHome,
                    )
                    is UiState.Success -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = NewsSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(NewsSpacing.lg),
                    ) {
                        items(items = s.data, key = { it.url }) { article ->
                            ArticleCard(
                                article = article,
                                variant = ArticleCardVariant.Standard,
                                modifier = Modifier.padding(horizontal = NewsSpacing.lg),
                                onClick = { onNavigateToArticle(article.url) },
                                isSaved = true,
                                onSave = { onEvent(SavedEvent.Delete(article)) },
                                onShare = {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, article.url)
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedHeader(count: Int) {
    val countLabel = when (count) {
        0 -> "NO ARTICLES YET"
        1 -> "1 ARTICLE"
        else -> "$count ARTICLES"
    }
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(NewsSpacing.lg)) {
        Text(
            "Saved",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(NewsSpacing.xs))
        Text(
            countLabel,
            style = MetaMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
