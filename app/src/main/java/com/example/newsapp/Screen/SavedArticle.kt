package com.example.newsapp.Screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.newsapp.ViewModel.SavedUiEvent
import com.example.newsapp.ViewModel.SavedViewModel
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.ArticleCard
import com.example.newsapp.ui.components.EmptyState
import com.example.newsapp.ui.components.ErrorState
import com.example.newsapp.ui.components.FeedSkeleton
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.components.enterpriseTopBarSpacing
import com.example.newsapp.ui.tokens.NewsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedArticle(navController: NavController) {
    val viewModel: SavedViewModel = hiltViewModel()
    val uiState by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SavedUiEvent.Message -> {
                    Toast.makeText(navController.context, event.value, Toast.LENGTH_SHORT).show()
                }

                is SavedUiEvent.UndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Removed from saved",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.article)
                    }
                }
            }
        }
    }

    NewsBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .enterpriseTopBarSpacing()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    UiState.Idle, UiState.Loading -> FeedSkeleton()
                    is UiState.Error -> ErrorState(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = {}
                    )

                    is UiState.Empty -> EmptyState(state.message)
                    is UiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = NewsSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm)
                        ) {
                            items(
                                items = state.data,
                                key = { article -> article.url }
                            ) { article ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { it * 0.25f }
                                )

                                LaunchedEffect(dismissState.currentValue) {
                                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                                        viewModel.delete(article)
                                    }
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        Text(
                                            text = "Delete",
                                            modifier = Modifier.padding(horizontal = NewsSpacing.lg, vertical = NewsSpacing.md),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true
                                ) {
                                    ArticleCard(
                                        article = article,
                                        onClick = { navController.navigateToArticleDetail(article.url) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
