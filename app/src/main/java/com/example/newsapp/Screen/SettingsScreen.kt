package com.example.newsapp.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newsapp.ViewModel.SettingsViewModel
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.*

@Composable
fun SettingsScreen(
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAlgorithm: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val highContrastEnabled by viewModel.highContrastEnabled.collectAsState()

    NewsBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()
            .padding(horizontal = NewsSpacing.lg)
            .padding(top = NewsSpacing.lg, bottom = NewsSpacing.xxl)
        ) {
            Text("Settings", style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(NewsSpacing.xs))
            Text("VERSION 2.4.1", style = MetaMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(NewsSpacing.xl))
            GroupLabel("PREFERENCES")
            SettingRow(Icons.Filled.Notifications, "Notification Preferences",
                "Manage alerts, topics, and quiet hours", onClick = onNavigateToNotifications)
            Spacer(Modifier.height(NewsSpacing.xs))
            SettingRow(Icons.Filled.Tune, "Feed Algorithm",
                "Customize topic weights and burst filter bubbles", onClick = onNavigateToAlgorithm)

            Spacer(Modifier.height(NewsSpacing.md))
            GroupLabel("ACCESSIBILITY")
            SettingRow(
                icon = Icons.Filled.Visibility,
                title = "High-Contrast Mode",
                subtitle = "Maximizes text legibility (WCAG AAA)",
                trailing = {
                    Switch(checked = highContrastEnabled, onCheckedChange = viewModel::toggleHighContrast)
                },
                onClick = { viewModel.toggleHighContrast(!highContrastEnabled) },
            )

            Spacer(Modifier.height(NewsSpacing.md))
            GroupLabel("ABOUT")
            SettingRow(Icons.Filled.CloudDone, "App status",
                "Offline cache and resilient networking are enabled")
            Spacer(Modifier.height(NewsSpacing.xs))
            SettingRow(Icons.Filled.Description, "Licenses",
                "Open-source dependencies", onClick = {})
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text, style = MetaMono,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = NewsSpacing.sm, vertical = NewsSpacing.sm),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = NewsSpacing.md, vertical = NewsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NewsSpacing.md),
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(NewsRadius.sm))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) trailing()
            else if (onClick != null) Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
            )
        }
    }
}