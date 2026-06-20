package com.example.newsapp.ui.components.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.newsapp.domain.reader.LineHeightOption
import com.example.newsapp.domain.reader.ReaderTheme
import com.example.newsapp.domain.reader.ReadingPreferences
import com.example.newsapp.domain.reader.WidthOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    prefs: ReadingPreferences,
    onFontScale: (Float) -> Unit,
    onLineHeight: (LineHeightOption) -> Unit,
    onWidth: (WidthOption) -> Unit,
    onTheme: (ReaderTheme) -> Unit,
    onBionic: (Boolean) -> Unit,
    onFocus: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Reading settings", style = MaterialTheme.typography.titleMedium)

            // Font size steppers (0.05 step, clamped in repo)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Text size", Modifier.weight(1f))
                OutlinedButton(onClick = { onFontScale(prefs.fontScale - 0.05f) }) { Text("A-") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onFontScale(prefs.fontScale + 0.05f) }) { Text("A+") }
            }

            SegmentedRow("Line height", LineHeightOption.entries, prefs.lineHeight, onLineHeight) { it.name.lowercase() }
            SegmentedRow("Width", WidthOption.entries, prefs.measureWidth, onWidth) { it.name.lowercase() }
            SegmentedRow("Theme", ReaderTheme.entries, prefs.theme, onTheme) { it.name.lowercase().replace('_', ' ') }

            ToggleRow("Bionic reading", prefs.bionicEnabled, onBionic)
            ToggleRow("Focus mode", prefs.focusEnabled, onFocus)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedRow(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit, name: (T) -> String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { i, opt ->
                SegmentedButton(
                    selected = opt == selected,
                    onClick = { onSelect(opt) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(name(opt)) }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
