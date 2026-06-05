package com.example.newsapp.Screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsapp.ui.tokens.NewsRadius
import com.example.newsapp.ui.tokens.NewsSpacing

@Composable
fun PrivacyConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        shape = RoundedCornerShape(NewsRadius.lg),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Privacy & Diagnostics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = "PulseNews can collect anonymous crash reports and usage " +
                    "telemetry to improve stability. We do not track your reading " +
                    "habits remotely or sell your data.\n\n" +
                    "You can change this choice anytime in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(
                    text = "Accept",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(
                    text = "Decline",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
