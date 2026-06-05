package com.example.newsapp.Screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.newsapp.ViewModel.SearchViewModel
import com.example.newsapp.module.Article
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.ArticleCard
import com.example.newsapp.ui.components.EmptyState
import com.example.newsapp.ui.components.ErrorState
import com.example.newsapp.ui.components.FeedSkeleton
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.components.PagingFooter
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.searchQuery.collectAsState()
    val trendingTopics by viewModel.trendingTopics.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    NewsBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                "Search",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                    SearchField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        onClear = viewModel::clearQuery,
                        onSubmit = {
                            viewModel.recordRecentSearch(query)
                            focusManager.clearFocus()
                        },
                        focusRequester = focusRequester,
                        modifier = Modifier
                            .padding(horizontal = NewsSpacing.lg)
                            .padding(bottom = NewsSpacing.sm),
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (query.isBlank()) {
                    EmptyQueryView(
                        recentSearches = recentSearches,
                        trendingTags = trendingTopics.map { it.tag },
                        onSearchSelected = { selected ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onQueryChange(selected)
                            viewModel.recordRecentSearch(selected)
                            focusManager.clearFocus()
                        },
                        onClearRecents = viewModel::clearRecentSearches,
                    )
                } else {
                    SearchResultsView(
                        results = searchResults,
                        query = query,
                        onArticleClick = { url -> navController.navigateToArticleDetail(url) },
                        onClearQuery = viewModel::clearQuery,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = NewsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(NewsSpacing.sm))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                "Search topics, sources, keywords",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyQueryView(
    recentSearches: List<String>,
    trendingTags: List<String>,
    onSearchSelected: (String) -> Unit,
    onClearRecents: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = NewsSpacing.lg)
            .padding(top = NewsSpacing.md, bottom = NewsSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(NewsSpacing.xl),
    ) {
        if (recentSearches.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "RECENT",
                        style = MetaMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onClearRecents) {
                        Text("Clear", style = MaterialTheme.typography.labelLarge)
                    }
                }
                recentSearches.forEach { term ->
                    RecentSearchRow(term = term, onClick = { onSearchSelected(term) })
                }
            }
        }

        if (trendingTags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(NewsSpacing.md)) {
                Text(
                    "TRENDING NOW",
                    style = MetaMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                ) {
                    trendingTags.forEach { tag ->
                        TrendingChip(label = tag, onClick = { onSearchSelected(tag) })
                    }
                }
            }
        }

        if (recentSearches.isEmpty() && trendingTags.isEmpty()) {
            Spacer(Modifier.height(NewsSpacing.xxl))
            EmptyState(
                title = "Search the news.",
                body = "Type a topic, source, or keyword to find articles across PulseNews.",
            )
        }
    }
}

@Composable
private fun RecentSearchRow(term: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NewsSpacing.md, vertical = NewsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NewsSpacing.md),
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = term,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TrendingChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NewsRadius.pill),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun SearchResultsView(
    results: LazyPagingItems<Article>,
    query: String,
    onArticleClick: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    val refresh = results.loadState.refresh
    val append = results.loadState.append

    when {
        refresh is LoadState.Loading && results.itemCount == 0 -> {
            FeedSkeleton()
        }
        refresh is LoadState.Error && results.itemCount == 0 -> {
            ErrorState(
                title = "Search failed.",
                body = refresh.error.localizedMessage
                    ?: "We couldn't reach the network. Try again in a moment.",
                retryable = true,
                onRetry = results::retry,
            )
        }
        refresh is LoadState.NotLoading && results.itemCount == 0 -> {
            EmptyState(
                title = "No matches for \u201C$query.\u201D",
                body = "Try a shorter or broader term, or clear the search to see trending topics.",
                actionText = "Show trending",
                onAction = onClearQuery,
            )
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(vertical = NewsSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = results.itemCount,
                    key = { index -> results.peek(index)?.url ?: "search-$index" },
                ) { index ->
                    val article = results[index]
                    if (article != null) {
                        ArticleCard(
                            article = article,
                            onClick = { onArticleClick(article.url ?: "") },
                        )
                    }
                }
                when {
                    append is LoadState.Error -> {
                        item {
                            ErrorState(
                                title = "Failed to load more",
                                body = append.error.localizedMessage
                                    ?: "Could not load more results.",
                                retryable = true,
                                onRetry = results::retry,
                            )
                        }
                    }
                    append is LoadState.Loading -> {
                        item { PagingFooter(isVisible = true) }
                    }
                    else -> {}
                }
            }
        }
    }
}
