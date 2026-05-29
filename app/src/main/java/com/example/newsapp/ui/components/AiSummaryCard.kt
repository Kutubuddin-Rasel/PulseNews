package com.example.newsapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsapp.ViewModel.AiState
import com.example.newsapp.ui.theme.AccentGradient
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@Composable
fun AiSummaryCard(aiState: AiState, modifier: Modifier = Modifier) {
    if (aiState is AiState.Idle) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(
                if (aiState is AiState.Error)
                    androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                else AccentGradient
            ))
            Column(Modifier.padding(NewsSpacing.lg)) {
                AiHeader(aiState)
                Spacer(Modifier.height(NewsSpacing.md))
                AnimatedContent(targetState = aiState, label = "AiSummaryContent") { state ->
                    when (state) {
                        is AiState.Idle -> {}
                        is AiState.Loading -> AiLoading()
                        is AiState.Success -> Text(
                            state.summary,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        is AiState.Error -> Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiHeader(state: AiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val isError = state is AiState.Error
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(NewsRadius.sm))
                .background(if (isError) MaterialTheme.colorScheme.errorContainer else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (!isError) {
                Box(Modifier.matchParentSize().background(AccentGradient, RoundedCornerShape(NewsRadius.sm)))
            }
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                contentDescription = if (isError) "Error" else "AI Generated",
                tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(NewsSpacing.md))
        Text(
            if (state is AiState.Error) "Couldn\u2019t generate summary" else "Gemini TL;DR",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (state is AiState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Text(
            when (state) {
                is AiState.Loading -> "reading\u2026"
                is AiState.Error   -> "retry available"
                is AiState.Success -> "AI"
                else -> ""
            },
            style = MetaMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiLoading() {
    val infinite = rememberInfiniteTransition("ai-load")
    val alpha by infinite.animateFloat(
        initialValue = 0.10f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth(0.85f).height(10.dp).clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth(0.96f).height(10.dp).clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth(0.60f).height(10.dp).clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp))
            .background(AccentGradient))
    }
}