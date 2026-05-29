package com.example.newsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeHeader(
    selectedCategoryId: Int,
    categories: List<Pair<Int, String>>,
    onCategoryClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFilters: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = NewsSpacing.md)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.lg),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Wordmark()
                Spacer(Modifier.height(NewsSpacing.xs))
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy")).uppercase(),
                    style = MetaMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NewsSpacing.xs)) {
                SquareIconButton(Icons.Filled.Tune, "Filters", primary = false, onClick = onOpenFilters)
                SquareIconButton(Icons.Filled.Refresh, "Refresh feed", primary = true, onClick = onRefresh)
            }
        }

        Spacer(Modifier.height(NewsSpacing.md))

        Surface(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.lg)
                .height(48.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(NewsRadius.md),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = NewsSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(NewsSpacing.sm))
                Text(
                    "Search topics, sources, keywords",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(NewsSpacing.md))

        CategoryStrip(selectedCategoryId, categories, onCategoryClick)
        Spacer(Modifier.height(NewsSpacing.sm))
    }
}

@Composable
private fun Wordmark() {
    val text = buildAnnotatedString {
        append("Pulse")
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("News") }
    }
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SquareIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        modifier = Modifier.size(40.dp).semantics { contentDescription = description },
        shape = RoundedCornerShape(NewsRadius.md),
        color = if (primary) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (primary) null else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null,
                tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CategoryStrip(selectedId: Int, items: List<Pair<Int, String>>, onClick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = NewsSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
    ) {
        items.forEach { (id, label) ->
            CategoryChip(label, selectedId == id) { onClick(id) }
        }
    }
}

@Composable
fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        shape = RoundedCornerShape(NewsRadius.pill),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.heightIn(min = 36.dp).semantics { contentDescription = text },
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedFilterBottomSheet(
    query: String,
    selectedSource: String?,
    availableSources: List<String>,
    onQueryChange: (String) -> Unit,
    onSourceChange: (String?) -> Unit,
    onSearch: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = NewsRadius.lg, topEnd = NewsRadius.lg),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.lg, vertical = NewsSpacing.md)) {
            Text("SOURCE", style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(NewsSpacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm)) {
                CategoryChip("All sources", selectedSource == null) { onSourceChange(null) }
                availableSources.forEach { src ->
                    CategoryChip(src, selectedSource == src) { onSourceChange(src) }
                }
            }

            Spacer(Modifier.height(NewsSpacing.xl))
            Text("KEYWORD", style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(NewsSpacing.sm))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. \"semiconductors\"") },
                shape = RoundedCornerShape(NewsRadius.md),
                trailingIcon = {
                    IconButton(onClick = { onSearch(); onDismissRequest() }) {
                        Icon(Icons.Filled.Search, contentDescription = "Submit search")
                    }
                },
            )
            Spacer(Modifier.height(NewsSpacing.xl))
        }
    }
}