package com.example.newsapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.newsapp.domain.model.VerificationStatus
import com.example.newsapp.module.Article
import com.example.newsapp.ui.theme.AccentGradient
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class ArticleCardVariant { Standard, Featured, Compact }

@Composable
fun NewsBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) { content() }
}

// ───────────────────────────────────────────────────────────
// ARTICLE CARD — one composable, three variants
// ───────────────────────────────────────────────────────────
@Composable
fun ArticleCard(
    article: Article,
    modifier: Modifier = Modifier,
    variant: ArticleCardVariant = ArticleCardVariant.Standard,
    onClick: () -> Unit,
    onSave: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    isSaved: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) NewsMotion.pressScale else 1f,
        animationSpec = tween(NewsMotion.fast, easing = NewsMotion.standardEasing),
        label = "press"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NewsSpacing.lg)
            .scale(scale)
            .clip(RoundedCornerShape(NewsRadius.card))
            .border(NewsStroke.thin, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(NewsRadius.card))
            .clickable(interactionSource = interaction, indication = LocalIndication.current) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics(mergeDescendants = true) {},
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.card),
    ) {
        Column {
            if (variant == ArticleCardVariant.Featured) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(AccentGradient))
            }
            Column(Modifier.padding(NewsSpacing.lg)) {
                if (variant != ArticleCardVariant.Compact) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(article.urlToImage).crossfade(true).build(),
                        contentDescription = "${article.title} thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (variant == ArticleCardVariant.Featured) NewsImage.featuredHeight else NewsImage.heroHeight)
                            .clip(RoundedCornerShape(NewsRadius.md))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Spacer(Modifier.height(NewsSpacing.md))
                }

                if (variant == ArticleCardVariant.Featured) {
                    Box(
                        Modifier.background(AccentGradient, RoundedCornerShape(NewsRadius.pill))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("TOP STORY", color = androidx.compose.ui.graphics.Color.White, style = MetaMono)
                    }
                    Spacer(Modifier.height(NewsSpacing.sm))
                }

                ArticleMetaRow(article)

                Spacer(Modifier.height(NewsSpacing.xs))
                Text(
                    text = article.title.orEmpty(),
                    style = if (variant == ArticleCardVariant.Featured)
                        MaterialTheme.typography.headlineMedium
                    else if (variant == ArticleCardVariant.Compact)
                        MaterialTheme.typography.titleLarge
                    else
                        MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (variant == ArticleCardVariant.Compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )

                if (variant != ArticleCardVariant.Compact && !article.description.isNullOrBlank()) {
                    Spacer(Modifier.height(NewsSpacing.xs))
                    Text(
                        text = article.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }

                if (variant != ArticleCardVariant.Compact) {
                    Spacer(Modifier.height(NewsSpacing.md))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(NewsSpacing.sm))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryPill(article.taxonomy?.firstOrNull() ?: "")
                        Row(horizontalArrangement = Arrangement.spacedBy(NewsSpacing.xs)) {
                            onSave?.let {
                                IconAction(
                                    icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    description = if (isSaved) "Saved" else "Save article",
                                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = it,
                                )
                            }
                            onShare?.let {
                                IconAction(Icons.Filled.IosShare, "Share", MaterialTheme.colorScheme.onSurfaceVariant, it)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleMetaRow(article: Article) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = article.source.name.uppercase(),
            style = MetaMono.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (article.provenance?.status == VerificationStatus.SOURCE_VERIFIED) {
            VerifiedDot()
        }
        Box(Modifier.size(2.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f)))
        Text(
            text = formatDate(article.publishedAt).uppercase(),
            style = MetaMono.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .75f)),
        )
    }
}

@Composable
private fun VerifiedDot() {
    Box(
        Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, androidx.compose.foundation.shape.CircleShape)
    )
}

@Composable
private fun CategoryPill(text: String) {
    if (text.isBlank()) return
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(NewsRadius.pill),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ───────────────────────────────────────────────────────────
// STATES — banner / empty / error / paging / skeleton
// ───────────────────────────────────────────────────────────
@Composable
fun StateBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = NewsSpacing.lg),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(NewsRadius.sm),
    ) {
        Row(
            Modifier.padding(horizontal = NewsSpacing.md, vertical = NewsSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.onSecondaryContainer))
            Spacer(Modifier.width(NewsSpacing.sm))
            Text(
                message,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.xl, vertical = NewsSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(NewsSpacing.xs))
            FilledTonalButton(onClick = onAction, shape = RoundedCornerShape(NewsRadius.pill)) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ErrorState(title: String, body: String, retryable: Boolean, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.xl, vertical = NewsSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
    ) {
        Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.error))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (retryable) {
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(NewsRadius.pill),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text("Retry now", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun FeedSkeleton(count: Int = 5) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = NewsSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
        modifier = Modifier.semantics { contentDescription = "Loading articles" }
    ) {
        items((0 until count).toList()) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    val infinite = rememberInfiniteTransition("skel")
    val alpha by infinite.animateFloat(
        initialValue = 0.08f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NewsSpacing.lg).clearAndSetSemantics { },
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(NewsSpacing.lg), verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm)) {
            Box(Modifier.fillMaxWidth().height(NewsImage.heroHeight).clip(RoundedCornerShape(NewsRadius.md))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
            Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(NewsRadius.xs))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
            Box(Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(NewsRadius.xs))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
            Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(NewsRadius.xs))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
        }
    }
}

@Composable
fun PagingFooter(isVisible: Boolean) {
    if (!isVisible) return
    val infinite = rememberInfiniteTransition("paging")
    Row(
        Modifier.fillMaxWidth().padding(vertical = NewsSpacing.md),
        horizontalArrangement = Arrangement.Center,
    ) {
        listOf(0, 150, 300).forEach { delay ->
            val a by infinite.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = NewsMotion.standardEasing, delayMillis = delay),
                    RepeatMode.Reverse,
                ),
                label = "dot$delay",
            )
            Box(
                Modifier.padding(horizontal = 3.dp).size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = a))
            )
        }
    }
}

// ───────────────────────────────────────────────────────────
fun formatDate(publishedAt: String?): String {
    if (publishedAt.isNullOrBlank()) return "Unknown"
    return try {
        val f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val dt = LocalDateTime.parse(publishedAt, f)
        val z = ZonedDateTime.of(dt, ZoneOffset.UTC)
        z.format(DateTimeFormatter.ofPattern("MMM d · HH:mm 'GMT'"))
    } catch (_: Exception) { "Unknown" }
}