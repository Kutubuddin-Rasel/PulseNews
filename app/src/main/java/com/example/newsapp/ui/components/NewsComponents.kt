package com.example.newsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.animateContentSize
import coil.compose.AsyncImage
import com.example.newsapp.module.Article
import com.example.newsapp.ui.tokens.NewsElevation
import com.example.newsapp.ui.tokens.NewsRadius
import com.example.newsapp.ui.tokens.NewsSpacing
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun NewsBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

@Composable
fun StateBanner(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NewsSpacing.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = NewsSpacing.md, vertical = NewsSpacing.sm)
        )
    }
}

@Composable
fun FeedSkeleton(count: Int = 5) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = NewsSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm)
    ) {
        items((0 until count).toList()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NewsSpacing.md),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(NewsSpacing.md)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                    Spacer(modifier = Modifier.height(NewsSpacing.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(14.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                    Spacer(modifier = Modifier.height(NewsSpacing.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NewsSpacing.xl, vertical = NewsSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(NewsSpacing.sm))
            AssistChip(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction()
                },
                label = { Text(actionText) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun ErrorState(message: String, retryable: Boolean, onRetry: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NewsSpacing.xl, vertical = NewsSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
        if (retryable) {
            Spacer(modifier = Modifier.height(NewsSpacing.sm))
            AssistChip(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRetry()
                },
                label = { Text("Retry") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    labelColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    }
}

private fun getRegionEmoji(regionCode: String?): String {
    return when(regionCode?.uppercase()) {
        "US" -> "🇺🇸"
        "UK", "GB" -> "🇬🇧"
        "BD" -> "🇧🇩"
        "IN" -> "🇮🇳"
        "AU" -> "🇦🇺"
        "CA" -> "🇨🇦"
        "JP" -> "🇯🇵"
        "SG" -> "🇸🇬"
        "DE" -> "🇩🇪"
        "FR" -> "🇫🇷"
        "IT" -> "🇮🇹"
        "ES" -> "🇪🇸"
        "BR" -> "🇧🇷"
        "MX" -> "🇲🇽"
        "ZA" -> "🇿🇦"
        "NG" -> "🇳🇬"
        "KE" -> "🇰🇪"
        "AE" -> "🇦🇪"
        "SA" -> "🇸🇦"
        "EG" -> "🇪🇬"
        "TR" -> "🇹🇷"
        "RU" -> "🇷🇺"
        "CN" -> "🇨🇳"
        "KR" -> "🇰🇷"
        "ID" -> "🇮🇩"
        "MY" -> "🇲🇾"
        "TH" -> "🇹🇭"
        "VN" -> "🇻🇳"
        "PH" -> "🇵🇭"
        "PK" -> "🇵🇰"
        "NZ" -> "🇳🇿"
        "GLOBAL" -> "🌍"
        else -> ""
    }
}

@Composable
fun ArticleCard(
    article: Article,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NewsSpacing.md)
            .minimumInteractiveComponentSize()
            .animateContentSize()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = NewsElevation.card)
    ) {
        Column(modifier = Modifier.padding(NewsSpacing.lg)) {
            if (!article.urlToImage.isNullOrBlank()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(article.urlToImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${article.title} thumbnail image",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.height(NewsSpacing.sm))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = article.source.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (article.provenance?.status == com.example.newsapp.domain.model.VerificationStatus.SOURCE_VERIFIED || article.sourceTier == 1 || article.sourceTier == 2) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = "Verified Publisher",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                if (!article.regionCode.isNullOrEmpty()) {
                    val emoji = getRegionEmoji(article.regionCode)
                    if (emoji.isNotEmpty()) {
                        Text(
                            text = " $emoji",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(
                    text = "  •  ${formatDate(article.publishedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(NewsSpacing.xs))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )

            if (!article.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(NewsSpacing.xs))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!article.taxonomy.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(NewsSpacing.sm))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(NewsSpacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(article.taxonomy) { tag ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = { /* TODO: Filter */ },
                            label = { Text(tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                            colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = null
                        )
                    }
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.height(NewsSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    trailing()
                }
            }
        }
    }
}

@Composable
fun PagingFooter(isVisible: Boolean) {
    if (!isVisible) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NewsSpacing.md),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
            strokeWidth = 2.dp
        )
    }
}

fun formatDate(publishedAt: String?): String {
    if (publishedAt.isNullOrBlank()) return "Unknown date"
    return try {
        // Try RFC_1123 first (e.g. "Sun, 24 May 2026 10:39:23 +0000")
        val zonedDateTime = try {
            ZonedDateTime.parse(publishedAt, DateTimeFormatter.RFC_1123_DATE_TIME)
        } catch (e: Exception) {
            // Fallback to ISO-8601 (e.g. "2026-05-24T12:00:28.472Z")
            ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)
        }
        val outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        zonedDateTime.format(outputFormatter)
    } catch (_: Exception) {
        "Unknown date"
    }
}
