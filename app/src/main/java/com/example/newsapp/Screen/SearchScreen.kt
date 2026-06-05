package com.example.newsapp.Screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.newsapp.ViewModel.SearchViewModel
import com.example.newsapp.navigateToArticleDetail
import com.example.newsapp.ui.components.ArticleCard
import com.example.newsapp.ui.components.CategoryChip
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.NewsRadius
import com.example.newsapp.ui.tokens.NewsSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val trendingTopics by viewModel.trendingTopics.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search news...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(NewsRadius.pill),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearQuery() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (query.isBlank()) {
                // Empty state: Show trending topics
                if (trendingTopics.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(NewsSpacing.lg)
                    ) {
                        Text(
                            "TRENDING NOW",
                            style = MetaMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(NewsSpacing.md))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm)
                        ) {
                            trendingTopics.forEach { topic ->
                                CategoryChip("#${topic.tag}", false) {
                                    viewModel.onQueryChange(topic.tag)
                                    focusManager.clearFocus()
                                }
                            }
                        }
                    }
                }
            } else {
                // Search results
                LazyColumn(
                    contentPadding = PaddingValues(NewsSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NewsSpacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = searchResults.itemCount,
                        key = { index -> searchResults[index]?.url ?: index }
                    ) { index ->
                        val article = searchResults[index]
                        if (article != null) {
                            ArticleCard(
                                article = article,
                                onClick = {
                                    navController.navigateToArticleDetail(article.url ?: "")
                                }
                            )
                        }
                    }

                    searchResults.apply {
                        when {
                            loadState.refresh is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            loadState.refresh is LoadState.Error -> {
                                val error = searchResults.loadState.refresh as LoadState.Error
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Error: ${error.error.localizedMessage}", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            loadState.append is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            loadState.refresh is LoadState.NotLoading && searchResults.itemCount == 0 -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(64.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No results found for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
