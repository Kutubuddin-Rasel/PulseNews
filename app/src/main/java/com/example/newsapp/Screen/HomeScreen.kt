package com.example.newsapp.Screen

import com.example.newsapp.Routes
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.newsapp.ViewModel.HomeViewModel
import com.example.newsapp.ViewModel.HomeEvent
import com.example.newsapp.ViewModel.HomeUiState
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.*
import com.example.newsapp.ui.tokens.NewsSpacing
import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.module.Article

@Composable
fun HomeRoute(
    navController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    // F2: collect the single filter-switching paging stream (no per-filter cached flows).
    val articles = vm.pagingData.collectAsLazyPagingItems()

    HomeScreen(
        state = state,
        articles = articles,
        onEvent = vm::onEvent,
        onNavigateToSearch = { navController.navigate(com.example.newsapp.Routes.search) },
        onNavigateToArticle = { navController.navigateToArticleDetail(it) },
        onNavigateToSaved = { navController.navigate(com.example.newsapp.Routes.saved) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    articles: LazyPagingItems<Article>,
    onEvent: (HomeEvent) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToArticle: (String) -> Unit,
    onNavigateToSaved: () -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showFilterSheet by remember { mutableStateOf(false) }

    val filter = state.filter
    val loadState = articles.loadState
    val isRefreshing = (loadState.refresh is androidx.paging.LoadState.Loading && articles.itemCount > 0) || state.isRefreshing

    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (wasRefreshing && !isRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        wasRefreshing = isRefreshing
    }
    
    val snackbar = com.example.newsapp.ui.components.LocalPulseSnackbar.current
    val scope = rememberCoroutineScope()

    // F3/F4: hoist the share-Intent construction into a single remembered handler keyed on context,
    // instead of allocating a fresh Intent-building closure inside every item on every recomposition.
    val onShareUrl = remember(context) {
        { url: String ->
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            context.startActivity(android.content.Intent.createChooser(sendIntent, null))
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbar.showSnackbar(it)
            onEvent(HomeEvent.SnackbarConsumed)
        }
    }

    LaunchedEffect(loadState.refresh) {
        if (loadState.refresh is androidx.paging.LoadState.Error && articles.itemCount > 0) {
            val err = loadState.refresh as androidx.paging.LoadState.Error
            snackbar.showSnackbar(err.error.localizedMessage ?: "Could not refresh articles")
        }
    }

    if (state.telemetryConsent == null) {
        PrivacyConsentDialog(
            onAccept = { onEvent(HomeEvent.SetTelemetryConsent(true)) }, 
            onDecline = { onEvent(HomeEvent.SetTelemetryConsent(false)) }
        )
    }

    val canRefresh = filter.selectedSource == null

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HomeHeader(
                    selectedCategoryKey = filter.categoryKey,
                    categories = state.dynamicCategories,
                    lastUpdated = state.lastUpdated,
                    onCategoryClick = { onEvent(HomeEvent.SetCategory(it)) },
                    onSearchClick = onNavigateToSearch,
                    onRefresh = {
                        if (canRefresh) {
                            articles.refresh()
                        } else {
                            scope.launch { snackbar.showSnackbar("Clear filters to refresh") }
                        }
                    },
                    onOpenFilters = { showFilterSheet = true },
                )
            },
            containerColor = Color.Transparent,
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { 
                    if (canRefresh) {
                        articles.refresh()
                    }
                },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                AnimatedContent(
                    targetState = filter,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + 
                         scaleIn(initialScale = 0.95f, animationSpec = tween(400, easing = LinearOutSlowInEasing))) togetherWith 
                        fadeOut(animationSpec = tween(200))
                    },
                    label = "category_feed_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetFilter ->
                    
                    when {
                        targetFilter.categoryKey == CategoryKey.FOR_YOU && !state.isAuthenticated -> {
                            EmptyState(
                                title = "Your Personalized News",
                                body = "Sign in to get a tailored feed of articles based on your reading history and preferences.",
                                actionText = "Sign in with Google",
                                onAction = {
                                    onEvent(HomeEvent.SignIn(context))
                                }
                            )
                        }
                        loadState.refresh is androidx.paging.LoadState.Loading && articles.itemCount == 0 -> FeedSkeleton()
                        loadState.refresh is androidx.paging.LoadState.Error && articles.itemCount == 0 -> {
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
                                onEvent(HomeEvent.SetCategory(CategoryKey.FOR_YOU))
                                onEvent(HomeEvent.SetSource(null))
                            },
                        )
                        else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(vertical = NewsSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                    ) {
                        items(
                            count = articles.itemCount,
                            // F3: key on the stable article url alone (drop the position suffix) so
                            // Compose preserves item state/animations across prepends and reorders.
                            key = { i -> articles.peek(i)?.url ?: i },
                            // F4: distinguish the hero card from standard rows so the lazy list only
                            // recycles compositions between like-typed items.
                            contentType = { i -> if (i == 0) "featured" else "standard" },
                        ) { i ->
                            articles[i]?.let { article ->
                                val variant = when {
                                    i == 0 -> ArticleCardVariant.Featured
                                    else -> ArticleCardVariant.Standard
                                }
                                ArticleCard(
                                    article = article,
                                    variant = variant,
                                    index = i,
                                    onClick = {
                                        onEvent(HomeEvent.TrackArticleClick(article.url ?: ""))
                                        onNavigateToArticle(article.url ?: "")
                                    },
                                    onSave = { 
                                        if (state.savedArticles.contains(article.url)) {
                                            onEvent(HomeEvent.DeleteArticle(article))
                                        } else {
                                            onEvent(HomeEvent.SaveArticle(article))
                                        }
                                    },
                                    onShare = { onShareUrl(article.url ?: "") },
                                    isSaved = state.savedArticles.contains(article.url)
                                )
                            }
                        }
                        val appendState = loadState.append
                        when {
                            appendState is androidx.paging.LoadState.Error -> {
                                item {
                                    ErrorState(
                                        title = "Failed to load more",
                                        body = appendState.error.localizedMessage ?: "Could not load more articles.",
                                        retryable = true,
                                        onRetry = articles::retry,
                                    )
                                }
                            }
                            appendState is androidx.paging.LoadState.Loading -> {
                                item { PagingFooter(isVisible = true) }
                            }
                            appendState is androidx.paging.LoadState.NotLoading
                                    && appendState.endOfPaginationReached
                                    && articles.itemCount > 0 -> {
                                item {
                                    EndOfFeedState(
                                        lastUpdated = state.lastUpdated,
                                        onBrowseSaved = onNavigateToSaved
                                    )
                                }
                                item { Spacer(Modifier.height(NewsSpacing.md)) }
                            }
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            SourceFilterBottomSheet(
                selectedSource = filter.selectedSource,
                availableSources = state.availableSources,
                onSourceChange = { onEvent(HomeEvent.SetSource(it)) },
                onDismissRequest = { showFilterSheet = false },
            )
        }
        }
    }
}