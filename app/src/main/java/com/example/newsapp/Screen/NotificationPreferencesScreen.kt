package com.example.newsapp.Screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.tokens.NewsSpacing
import com.example.newsapp.ui.viewmodel.NotificationPreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationPreferencesViewModel = hiltViewModel()
) {
    val subscribedTopics by viewModel.subscribedTopics.collectAsState()
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsState()
    val quietHoursStartMinutes by viewModel.quietHoursStartMinutes.collectAsState()
    val quietHoursEndMinutes by viewModel.quietHoursEndMinutes.collectAsState()
    val maxDailyNotifications by viewModel.maxDailyNotifications.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result if needed
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        NewsBackground(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(NewsSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(NewsSpacing.lg)
            ) {
                Text(
                    text = "Manage your breaking news and alerts to prevent notification fatigue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TopicSection(
                    subscribedTopics = subscribedTopics,
                    onToggleTopic = viewModel::toggleTopic
                )

                ThrottlingSection(
                    maxDailyNotifications = maxDailyNotifications,
                    onMaxChanged = viewModel::setMaxDailyNotifications
                )

                QuietHoursSection(
                    enabled = quietHoursEnabled,
                    startMinutes = quietHoursStartMinutes,
                    endMinutes = quietHoursEndMinutes,
                    onEnabledChange = viewModel::setQuietHoursEnabled,
                    onTimeChange = viewModel::setQuietHours
                )
            }
        }
    }
}

@Composable
fun TopicSection(subscribedTopics: Set<String>, onToggleTopic: (String, Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(NewsSpacing.lg)) {
            Text(
                text = "Topics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(NewsSpacing.sm))

            val availableTopics = listOf("Technology", "Politics", "Business", "Local News")

            availableTopics.forEach { topic ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = topic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = subscribedTopics.contains(topic),
                        onCheckedChange = { onToggleTopic(topic, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThrottlingSection(maxDailyNotifications: Int, onMaxChanged: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(NewsSpacing.lg)) {
            Text(
                text = "Daily Limit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Maximum notifications: $maxDailyNotifications",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = maxDailyNotifications.toFloat(),
                onValueChange = { onMaxChanged(it.toInt()) },
                valueRange = 0f..20f,
                steps = 19
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuietHoursSection(
    enabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(NewsSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quiet Hours",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(NewsSpacing.sm))
                val formatTime: (Int) -> String = { minutes ->
                    val h = minutes / 60
                    val m = minutes % 60
                    val period = if (h >= 12) "PM" else "AM"
                    val displayH = if (h % 12 == 0) 12 else h % 12
                    String.format("%02d:%02d %s", displayH, m, period)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Start: ${formatTime(startMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "End: ${formatTime(endMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
