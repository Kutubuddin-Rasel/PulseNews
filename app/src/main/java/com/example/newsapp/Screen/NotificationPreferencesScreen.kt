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
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.newsapp.ui.theme.MetaMono
import com.example.newsapp.ui.tokens.NewsRadius

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

    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
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
                if (!permissionGranted) {
                    PermissionBanner(
                        onEnable = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }

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
    onTimeChange: (Int, Int) -> Unit,
) {
    var editing by remember { mutableStateOf<QuietHoursEdit?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(NewsSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Quiet Hours",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Silence notifications between these times.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(Modifier.height(NewsSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NewsSpacing.sm),
                ) {
                    QuietHoursTimeChip(
                        label = "FROM",
                        minutes = startMinutes,
                        onClick = { editing = QuietHoursEdit.Start },
                        modifier = Modifier.weight(1f),
                    )
                    QuietHoursTimeChip(
                        label = "UNTIL",
                        minutes = endMinutes,
                        onClick = { editing = QuietHoursEdit.End },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    editing?.let { which ->
        val initialMinutes = if (which == QuietHoursEdit.Start) startMinutes else endMinutes
        val timeState = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { editing = null },
            title = {
                Text(
                    if (which == QuietHoursEdit.Start) "Quiet hours start" else "Quiet hours end",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val chosen = timeState.hour * 60 + timeState.minute
                    if (which == QuietHoursEdit.Start) onTimeChange(chosen, endMinutes)
                    else onTimeChange(startMinutes, chosen)
                    editing = null
                }) {
                    Text("Set", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

private sealed interface QuietHoursEdit {
    data object Start : QuietHoursEdit
    data object End : QuietHoursEdit
}

@Composable
private fun QuietHoursTimeChip(
    label: String,
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val h = minutes / 60
    val m = minutes % 60
    val period = if (h >= 12) "PM" else "AM"
    val displayH = if (h % 12 == 0) 12 else h % 12
    val timeText = String.format("%02d:%02d %s", displayH, m, period)

    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(NewsSpacing.md)) {
            Text(
                text = label,
                style = MetaMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(NewsSpacing.xs))
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PermissionBanner(onEnable: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(NewsRadius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(NewsSpacing.lg)) {
            Text(
                "NOTIFICATIONS DISABLED",
                style = MetaMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(NewsSpacing.sm))
            Text(
                "PulseNews can't deliver alerts until you enable system notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(NewsSpacing.md))
            Button(
                onClick = onEnable,
                shape = RoundedCornerShape(NewsRadius.pill),
            ) {
                Text("Enable notifications", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
