package com.example.newsapp.Screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.newsapp.ViewModel.FeedMode
import com.example.newsapp.ViewModel.HomeViewModel
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.ArticleCard
import com.example.newsapp.ui.components.EmptyState
import com.example.newsapp.ui.components.ErrorState
import com.example.newsapp.ui.components.FeedSkeleton
import com.example.newsapp.ui.components.HomeHeader
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.components.PagingFooter
import com.example.newsapp.ui.components.StateBanner
import com.example.newsapp.ui.tokens.NewsSpacing
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val categories = listOf(
    "business",
    "entertainment",
    "general",
    "health",
    "science",
    "sports",
    "technology"
)

private val sortOptions = listOf("relevancy", "popularity", "publishedAt")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showFilterSheet by remember { mutableStateOf(false) }

    val telemetryConsent by viewModel.telemetryConsent.collectAsState()
    val haptic = LocalHapticFeedback.current

    var isHeaderVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var previousIndex = 0
        var previousScrollOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > previousIndex || (index == previousIndex && offset > previousScrollOffset + 10)) {
                    if (index > 0) isHeaderVisible = false
                } else if (index < previousIndex || (index == previousIndex && offset < previousScrollOffset - 10)) {
                    isHeaderVisible = true
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (telemetryConsent == null) {
        PrivacyConsentDialog(
            onAccept = { viewModel.setTelemetryConsent(true) },
            onDecline = { viewModel.setTelemetryConsent(false) }
        )
    }

    NewsBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AnimatedVisibility(
                    visible = isHeaderVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    HomeHeader(
                        onRefresh = viewModel::refresh,
                        scrollBehavior = scrollBehavior
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val articles = viewModel.feed.androidx.paging.compose.collectAsLazyPagingItems()
                    val loadState = articles.loadState
                    
                    if (loadState.refresh is androidx.paging.LoadState.Loading) {
                        FeedSkeleton()
                    } else if (loadState.refresh is androidx.paging.LoadState.Error) {
                        val error = loadState.refresh as androidx.paging.LoadState.Error
                        ErrorState(
                            message = error.error.localizedMessage ?: "Unknown error",
                            retryable = true,
                            onRetry = { articles.retry() }
                        )
                    } else if (articles.itemCount == 0) {
                        EmptyState(
                            message = "No articles found.",
                            actionText = "Refresh",
                            onAction = { articles.refresh() }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = NewsSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm)
                        ) {
                            items(
                                count = articles.itemCount,
                                key = { index -> articles.peek(index)?.url ?: index }
                            ) { index ->
                                val article = articles[index]
                                if (article != null) {
                                    ArticleCard(
                                        article = article,
                                        onClick = { navController.navigateToArticleDetail(article.url) }
                                    )
                                }
                            }
                            item {
                                PagingFooter(isVisible = loadState.append is androidx.paging.LoadState.Loading)
                            }
                            if (loadState.append is androidx.paging.LoadState.Error) {
                                item {
                                    ErrorState(
                                        message = "Error loading more",
                                        retryable = true,
                                        onRetry = { articles.retry() }
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
