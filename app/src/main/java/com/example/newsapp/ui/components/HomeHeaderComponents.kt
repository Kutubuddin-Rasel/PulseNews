package com.example.newsapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

import com.example.newsapp.domain.model.CategoryKey

@Composable
fun HomeHeader(
    selectedCategoryKey: CategoryKey,
    categories: List<Pair<CategoryKey, String>>,
    lastUpdated: String?,
    onCategoryClick: (CategoryKey) -> Unit,
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
                val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy")).uppercase()
                val updatedStr = if (lastUpdated != null) {
                    val relativeTime = com.example.newsapp.ui.components.formatDate(lastUpdated).lowercase()
                    " • Updated $relativeTime"
                } else ""
                
                Text(
                    "$dateStr$updatedStr",
                    style = MetaMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NewsSpacing.xs)) {
                SquareIconButton(Icons.Filled.Tune, "Filters", primary = false, onClick = onOpenFilters)
                SquareIconButton(Icons.Filled.Refresh, "Refresh feed", primary = false, onClick = onRefresh)
            }
        }

        Spacer(Modifier.height(NewsSpacing.md))

        Surface(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.lg)
                .height(48.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.medium,
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

        CategoryStrip(selectedCategoryKey, categories, onCategoryClick)
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
        shape = MaterialTheme.shapes.medium,
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
private fun CategoryStrip(
    selectedKey: CategoryKey,
    items: List<Pair<CategoryKey, String>>,
    onClick: (CategoryKey) -> Unit,
) {
    val scrollState = rememberScrollState()
    val background = MaterialTheme.colorScheme.background

    val leftFadeAlpha by animateFloatAsState(
        targetValue = if (scrollState.canScrollBackward) 1f else 0f,
        animationSpec = tween(
            durationMillis = NewsMotion.normal,
            easing = NewsMotion.standardEasing,
        ),
        label = "categoryLeftFade",
    )
    val rightFadeAlpha by animateFloatAsState(
        targetValue = if (scrollState.canScrollForward) 1f else 0f,
        animationSpec = tween(
            durationMillis = NewsMotion.normal,
            easing = NewsMotion.standardEasing,
        ),
        label = "categoryRightFade",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = NewsSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
        ) {
            items.forEach { (key, label) ->
                CategoryChip(label, selectedKey == key) { onClick(key) }
            }
        }

        Row(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(28.dp)
                    .graphicsLayer { alpha = leftFadeAlpha }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                background,
                                background.copy(alpha = 0f),
                            ),
                        ),
                    ),
            )

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(28.dp)
                    .graphicsLayer { alpha = rightFadeAlpha }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                background.copy(alpha = 0f),
                                background,
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "chipContainerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "chipBorderColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "chipTextColor"
    )

    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.heightIn(min = 36.dp).semantics { contentDescription = text },
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceFilterBottomSheet(
    selectedSource: String?,
    availableSources: List<String>,
    onSourceChange: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NewsSpacing.lg, vertical = NewsSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOURCE FILTER", style = MetaMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selectedSource != null) {
                    TextButton(
                        onClick = { onSourceChange(null); onDismissRequest() },
                        contentPadding = PaddingValues(horizontal = NewsSpacing.md)
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                item {
                    SourceListItem(
                        name = "All sources",
                        selected = selectedSource == null,
                        onClick = { onSourceChange(null); onDismissRequest() }
                    )
                }
                items(availableSources) { src ->
                    SourceListItem(
                        name = src,
                        selected = selectedSource == src,
                        onClick = { onSourceChange(src); onDismissRequest() }
                    )
                }
                item {
                    Spacer(Modifier.height(NewsSpacing.xl))
                }
            }
        }
    }
}

@Composable
private fun SourceListItem(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = NewsSpacing.lg, vertical = NewsSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}