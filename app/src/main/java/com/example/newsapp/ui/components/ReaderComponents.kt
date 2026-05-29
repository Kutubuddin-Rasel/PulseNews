package com.example.newsapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.newsapp.ui.theme.AccentGradient
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    sourceLabel: String,
    isSaved: Boolean,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    sourceLabel.uppercase(),
                    style = MetaMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            navigationIcon = {
                ReaderActionButton("Go back", onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            actions = {
                ReaderActionButton(if (isSaved) "Remove from saved" else "Save article", onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                ReaderActionButton("Share article", onShare) { Icon(Icons.Filled.IosShare, contentDescription = null) }
                ReaderActionButton("Open in browser", onOpenExternal) { Icon(Icons.Filled.OpenInBrowser, contentDescription = null) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
fun ReaderActionButton(
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
        content = content,
    )
}

// Aurora-tinted reading progress strip (0f..1f)
@Composable
fun ReaderProgressStrip(progress: Float, isLoading: Boolean = false) {
    if (isLoading) {
        val infinite = rememberInfiniteTransition("reader-load")
        val x by infinite.animateFloat(
            initialValue = -1f, targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
            label = "x",
        )
        Box(
            Modifier.fillMaxWidth().height(2.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(
                Modifier.fillMaxWidth(0.3f).fillMaxHeight()
                    .background(AccentGradient)
            )
        }
    } else {
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "reading_progress"
        )
        Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Box(
                Modifier.fillMaxWidth(animatedProgress).fillMaxHeight()
                    .background(AccentGradient)
            )
        }
    }
}

@Composable
fun ReaderErrorPanel(
    message: String,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(NewsSpacing.lg),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(NewsRadius.card),
    ) {
        Column(
            Modifier.padding(NewsSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm)) {
                Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.error))
                Text("Article failed to load", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm)) {
                Button(onClick = onRetry, shape = RoundedCornerShape(NewsRadius.pill)) {
                    Text("Retry")
                }
                OutlinedButton(onClick = onOpenExternal, shape = RoundedCornerShape(NewsRadius.pill)) {
                    Text("Open in browser")
                }
            }
        }
    }
}