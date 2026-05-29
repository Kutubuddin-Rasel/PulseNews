package com.example.newsapp.Screen

import android.widget.Toast
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
import com.example.newsapp.ViewModel.SavedUiEvent
import com.example.newsapp.ViewModel.SavedViewModel
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.*
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedArticle(navController: NavController) {
    val vm: SavedViewModel = hiltViewModel()
    val uiState by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is SavedUiEvent.Message -> Toast.makeText(navController.context, event.value, Toast.LENGTH_SHORT).show()
                is SavedUiEvent.UndoDelete -> {
                    val r = snackbarHostState.showSnackbar("Removed from saved", "Undo", duration = SnackbarDuration.Short)
                    if (r == SnackbarResult.ActionPerformed) vm.undoDelete(event.article)
                }
            }
        }
    }

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { SavedHeader(count = (uiState as? UiState.Success)?.data?.size ?: 0) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (val s = uiState) {
                    UiState.Idle, UiState.Loading -> FeedSkeleton()
                    is UiState.Error -> ErrorState(
                        title = "Couldn’t load saved.",
                        body = s.message, retryable = s.retryable, onRetry = {},
                    )
                    is UiState.Empty -> EmptyState(
                        title = "Nothing in ‘Saved’ yet.",
                        body = "Bookmark stories you want to come back to. They’ll appear here, available offline.",
                    )
                    is UiState.Success -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = NewsSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                    ) {
                        items(items = s.data, key = { it.url }) { article ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { it * 0.25f },
                                confirmValueChange = { v ->
                                    if (v != SwipeToDismissBoxValue.Settled) { vm.delete(article); true } else false
                                },
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { SwipeDeleteBackground() },
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = true,
                            ) {
                                ArticleCard(
                                    article = article,
                                    variant = ArticleCardVariant.Compact,
                                    onClick = { navController.navigateToArticleDetail(article.url) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedHeader(count: Int) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(NewsSpacing.lg)) {
        Text("Saved", style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(NewsSpacing.xs))
        Text("$count articles".uppercase(), style = MetaMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwipeDeleteBackground() {
    Surface(
        modifier = Modifier.fillMaxSize().padding(horizontal = NewsSpacing.lg),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(NewsRadius.card),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = NewsSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text("DELETE", style = MetaMono, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}