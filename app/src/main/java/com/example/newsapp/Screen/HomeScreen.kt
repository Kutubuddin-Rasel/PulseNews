package com.example.newsapp.Screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.newsapp.ViewModel.HomeViewModel
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.*
import com.example.newsapp.ui.tokens.NewsSpacing

private val CATEGORIES = listOf(
    1 to "For You", 2 to "Technology", 3 to "Business",
    4 to "Politics", 5 to "Sports", 6 to "Entertainment", 7 to "Health",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val vm: HomeViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showFilterSheet by remember { mutableStateOf(false) }

    val telemetryConsent by vm.telemetryConsent.collectAsState()
    val articles = vm.feed.collectAsLazyPagingItems()
    val loadState = articles.loadState
    val isRefreshing = loadState.refresh is androidx.paging.LoadState.Loading

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(vm) {
        vm.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    if (telemetryConsent == null) {
        PrivacyConsentDialog(onAccept = { vm.setTelemetryConsent(true) }, onDecline = { vm.setTelemetryConsent(false) })
    }

    val canRefresh = uiState.filter.categoryId == 1 &&
        uiState.filter.activeQuery.isEmpty() && uiState.filter.selectedSource == null

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                HomeHeader(
                    selectedCategoryId = uiState.filter.categoryId,
                    categories = CATEGORIES,
                    onCategoryClick = vm::setCategory,
                    onSearchClick = { showFilterSheet = true },
                    onRefresh = {
                        if (canRefresh) articles.refresh()
                        else Toast.makeText(context, "Refresh only in ‘For You’", Toast.LENGTH_SHORT).show()
                    },
                    onOpenFilters = { showFilterSheet = true },
                )
            },
            containerColor = Color.Transparent,
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { if (canRefresh) articles.refresh() },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                when {
                    loadState.refresh is androidx.paging.LoadState.Loading -> FeedSkeleton()
                    loadState.refresh is androidx.paging.LoadState.Error -> {
                        val err = loadState.refresh as androidx.paging.LoadState.Error
                        ErrorState(
                            title = "We lost the signal.",
                            body = err.error.localizedMessage ?: "PulseNews can’t reach the network right now.",
                            retryable = true, onRetry = articles::retry,
                        )
                    }
                    articles.itemCount == 0 -> EmptyState(
                        title = "Nothing here yet.",
                        body = "Try clearing your filters or switching back to For You.",
                        actionText = "Reset filters",
                        onAction = {
                            vm.setCategory(1); vm.setSource(null)
                            vm.updateQueryInput(""); vm.submitSearch()
                        },
                    )
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = NewsSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                    ) {
                        items(
                            count = articles.itemCount,
                            key = { i -> articles.peek(i)?.url ?: i },
                        ) { i ->
                            articles[i]?.let { article ->
                                val variant = when {
                                    i == 0 -> ArticleCardVariant.Featured
                                    else -> ArticleCardVariant.Standard
                                }
                                ArticleCard(
                                    article = article,
                                    variant = variant,
                                    onClick = {
                                        vm.trackArticleClick(article.url ?: "")
                                        navController.navigateToArticleDetail(article.url ?: "")
                                    },
                                    onSave = { vm.saveArticle(article) },
                                    onShare = {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, article.url ?: "")
                                            type = "text/plain"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                    },
                                )
                            }
                        }
                        item { PagingFooter(isVisible = loadState.append is androidx.paging.LoadState.Loading) }
                    }
                }
            }

            if (showFilterSheet) {
                val sources by vm.availableSources.collectAsState()
                FeedFilterBottomSheet(
                    query = uiState.filter.queryInput,
                    selectedSource = uiState.filter.selectedSource,
                    availableSources = sources,
                    onQueryChange = vm::updateQueryInput,
                    onSourceChange = vm::setSource,
                    onSearch = vm::submitSearch,
                    onDismissRequest = { showFilterSheet = false },
                )
            }
        }
    }
}