package com.example.newsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.newsapp.ViewModel.FeedMode
import com.example.newsapp.ui.tokens.NewsSpacing

fun Modifier.enterpriseTopBarSpacing(): Modifier {
    return this
        .statusBarsPadding()
        .padding(top = NewsSpacing.sm)
}

@Composable
fun InsetAwareHeaderContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .enterpriseTopBarSpacing()
            .padding(start = NewsSpacing.lg, end = NewsSpacing.lg, bottom = NewsSpacing.md),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHeader(
    onRefresh: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "PulseNews",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Real-time coverage with offline resilience",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = NewsSpacing.xs)
                )
            }
        },
        actions = {
            HeaderActionButton(onRefresh = onRefresh)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedFilterBottomSheet(
    mode: FeedMode,
    selectedCategory: String,
    currentSort: String,
    query: String,
    onModeChange: (FeedMode) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    categories: List<String>,
    sortOptions: List<String>,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NewsSpacing.lg, vertical = NewsSpacing.md)
        ) {
            ModeSwitcher(mode = mode, onModeChange = onModeChange)

            Spacer(modifier = Modifier.height(NewsSpacing.md))

            if (mode == FeedMode.Top) {
                CategoryChipRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChange = onCategoryChange
                )
            } else {
                SearchAndSortBlock(
                    query = query,
                    currentSort = currentSort,
                    sortOptions = sortOptions,
                    onQueryChange = onQueryChange,
                    onSearch = {
                        onSearch()
                        onDismissRequest()
                    },
                    onSortChange = onSortChange
                )
            }
            Spacer(modifier = Modifier.height(NewsSpacing.xl))
        }
    }
}

@Composable
fun HeaderActionButton(onRefresh: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onRefresh()
        },
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Refresh feed" },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
    }
}

@Composable
fun ModeSwitcher(mode: FeedMode, onModeChange: (FeedMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm)) {
        EnterprisePill(
            text = "Top",
            selected = mode == FeedMode.Top,
            onClick = { onModeChange(FeedMode.Top) }
        )
        EnterprisePill(
            text = "Everything",
            selected = mode == FeedMode.Everything,
            onClick = { onModeChange(FeedMode.Everything) }
        )
    }
}

@Composable
fun CategoryChipRow(
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    FadedHorizontalRow {
        categories.forEach { category ->
            EnterprisePill(
                text = category.replaceFirstChar(Char::uppercase),
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) }
            )
        }
    }
}

@Composable
fun SortChipRow(
    sortOptions: List<String>,
    currentSort: String,
    onSortChange: (String) -> Unit
) {
    FadedHorizontalRow {
        sortOptions.forEach { sortBy ->
            EnterprisePill(
                text = sortBy,
                selected = currentSort == sortBy,
                onClick = { onSortChange(sortBy) }
            )
        }
    }
}

@Composable
private fun SearchAndSortBlock(
    query: String,
    currentSort: String,
    sortOptions: List<String>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSortChange: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        singleLine = true,
        placeholder = { Text("Search topic") },
        trailingIcon = {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSearch()
                },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Submit search" }
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(NewsSpacing.sm))

    SortChipRow(
        sortOptions = sortOptions,
        currentSort = currentSort,
        onSortChange = onSortChange
    )
}

@Composable
private fun EnterprisePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (selected) 1.dp else 0.dp,
        shadowElevation = if (selected) 0.dp else 1.dp,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.large)
            .semantics { contentDescription = text }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = NewsSpacing.lg, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FadedHorizontalRow(content: @Composable RowScope.() -> Unit) {
    val scrollState = rememberScrollState()
    var showStartFade by remember { mutableStateOf(false) }
    var showEndFade by remember { mutableStateOf(true) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (value, max) ->
                showStartFade = value > 0
                showEndFade = value < max
            }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(start = NewsSpacing.xs, end = NewsSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
            content = content
        )

        if (showStartFade) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp)
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.background, Color.Transparent))
                    )
            )
        }

        if (showEndFade) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp)
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))
                    )
            )
        }
    }
}
