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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.newsapp.ViewModel.HomeViewModel
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.*
import com.example.newsapp.ui.tokens.NewsSpacing
import com.example.newsapp.domain.model.CategoryKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val vm: HomeViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showFilterSheet by remember { mutableStateOf(false) }

    val telemetryConsent by vm.telemetryConsent.collectAsState()
    val isAuthenticated by vm.isAuthenticated.collectAsState()
    val savedArticles by vm.savedArticles.collectAsState()
    val categories by vm.dynamicCategories.collectAsState()
    val lastUpdated by vm.lastUpdated.collectAsState()
    val trendingTopics by vm.trendingTopics.collectAsState()
    val currentFeed = remember(uiState.filter, isAuthenticated) { vm.getFeed(uiState.filter) }
    val articles = currentFeed.collectAsLazyPagingItems()
    val loadState = articles.loadState
    val isRefreshing = (loadState.refresh is androidx.paging.LoadState.Loading && articles.itemCount > 0) || uiState.isRefreshing

    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (wasRefreshing && !isRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        wasRefreshing = isRefreshing
    }
    val snackbar = com.example.newsapp.ui.components.LocalPulseSnackbar.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.events.collect { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(loadState.refresh) {
        if (loadState.refresh is androidx.paging.LoadState.Error && articles.itemCount > 0) {
            val err = loadState.refresh as androidx.paging.LoadState.Error
            snackbar.showSnackbar(err.error.localizedMessage ?: "Could not refresh articles")
        }
    }

    if (telemetryConsent == null) {
        PrivacyConsentDialog(onAccept = { vm.setTelemetryConsent(true) }, onDecline = { vm.setTelemetryConsent(false) })
    }

    val canRefresh = uiState.filter.selectedSource == null

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HomeHeader(
                    selectedCategoryKey = uiState.filter.categoryKey,
                    categories = categories,
                    lastUpdated = lastUpdated,
                    onCategoryClick = vm::setCategory,
                    onSearchClick = { navController.navigate(com.example.newsapp.Routes.search) },
                    onRefresh = {
                        if (canRefresh) {
                            vm.fetchNewsMeta()
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
                    targetState = uiState.filter,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + 
                         scaleIn(initialScale = 0.95f, animationSpec = tween(400, easing = LinearOutSlowInEasing))) togetherWith 
                        fadeOut(animationSpec = tween(200))
                    },
                    label = "category_feed_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetFilter ->
                    val targetFeed = remember(targetFilter, isAuthenticated) { vm.getFeed(targetFilter) }
                    val targetArticles = targetFeed.collectAsLazyPagingItems()
                    val targetLoadState = targetArticles.loadState
                    
                    when {
                        targetFilter.categoryKey == CategoryKey.FOR_YOU && !isAuthenticated -> {
                            EmptyState(
                                title = "Your Personalized News",
                                body = "Sign in to get a tailored feed of articles based on your reading history and preferences.",
                                actionText = "Sign in with Google",
                                onAction = {
                                    vm.signIn(context)
                                }
                            )
                        }
                        targetLoadState.refresh is androidx.paging.LoadState.Loading && targetArticles.itemCount == 0 -> FeedSkeleton()
                        targetLoadState.refresh is androidx.paging.LoadState.Error && targetArticles.itemCount == 0 -> {
                            val err = targetLoadState.refresh as androidx.paging.LoadState.Error
                            ErrorState(
                                title = "We lost the signal.",
                                body = err.error.localizedMessage ?: "PulseNews can’t reach the network right now.",
                                retryable = true, onRetry = targetArticles::retry,
                            )
                        }
                        targetArticles.itemCount == 0 -> EmptyState(
                            title = "Nothing here yet.",
                            body = "Try clearing your filters or switching back to For You.",
                            actionText = "Reset filters",
                            onAction = {
                                vm.setCategory(CategoryKey.FOR_YOU); vm.setSource(null)
                            },
                        )
                        else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(vertical = NewsSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                    ) {
                        items(
                            count = targetArticles.itemCount,
                            key = { i -> "${targetArticles.peek(i)?.url}_$i" },
                        ) { i ->
                            targetArticles[i]?.let { article ->
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
                                    onSave = { 
                                        if (vm.savedArticles.value.contains(article.url)) {
                                            vm.deleteArticle(article)
                                        } else {
                                            vm.saveArticle(article)
                                        }
                                    },
                                    onShare = {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, article.url ?: "")
                                            type = "text/plain"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                    },
                                    isSaved = savedArticles.contains(article.url)
                                )
                            }
                        }
                        val appendState = targetLoadState.append
                        when {
                            appendState is androidx.paging.LoadState.Error -> {
                                item {
                                    ErrorState(
                                        title = "Failed to load more",
                                        body = appendState.error.localizedMessage ?: "Could not load more articles.",
                                        retryable = true,
                                        onRetry = targetArticles::retry,
                                    )
                                }
                            }
                            appendState is androidx.paging.LoadState.Loading -> {
                                item { PagingFooter(isVisible = true) }
                            }
                            appendState is androidx.paging.LoadState.NotLoading
                                    && appendState.endOfPaginationReached
                                    && targetArticles.itemCount > 0 -> {
                                item {
                                    EndOfFeedState(
                                        lastUpdated = lastUpdated,
                                        onBrowseSaved = {
                                            navController.navigate(Routes.saved)
                                        }
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
                val sources by vm.availableSources.collectAsState()
                SourceFilterBottomSheet(
                    selectedSource = uiState.filter.selectedSource,
                    availableSources = sources,
                    onSourceChange = vm::setSource,
                    onDismissRequest = { showFilterSheet = false },
                )
            }
        }
    }
}