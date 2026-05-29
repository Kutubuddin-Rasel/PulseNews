package com.example.newsapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.newsapp.ViewModel.AiState
import com.example.newsapp.ui.tokens.NewsSpacing

@Composable
fun AiSummaryCard(
    aiState: AiState,
    modifier: Modifier = Modifier
) {
    if (aiState is AiState.Idle) return

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(NewsSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Generated",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(NewsSpacing.sm))
                Text(
                    text = "Gemini TL;DR",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(NewsSpacing.md))

            AnimatedContent(targetState = aiState, label = "AiSummaryContent") { state ->
                when (state) {
                    is AiState.Idle -> { /* Never reached due to early return */ }
                    is AiState.Loading -> {
                        Column {
                            Text(
                                text = "Reading article to generate summary...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(NewsSpacing.sm))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    is AiState.Success -> {
                        Text(
                            text = state.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    is AiState.Error -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(NewsSpacing.sm))
                            Text(
                                text = "Could not generate summary: ${state.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
