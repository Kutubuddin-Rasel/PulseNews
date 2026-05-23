package com.example.newsapp.Screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


@Composable
fun PrivacyConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Block dismiss to enforce a choice */ },
        title = {
            Text(
                text = "Privacy & Diagnostics",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "To improve your experience, PulseNews would like to collect anonymous crash reports and usage telemetry. This data is strictly used for stability improvements. We do not track your reading habits remotely or sell your data.\\n\\nDo you consent to anonymous diagnostic telemetry?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Accept", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDecline) {
                Text("Decline", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
