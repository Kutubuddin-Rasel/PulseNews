package com.example.newsapp.Screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ViewModel.AlgorithmPreferencesViewModel
import com.example.newsapp.ViewModel.AlgorithmPreferencesEvent
import com.example.newsapp.ViewModel.AlgorithmWeightsUiState
import com.example.newsapp.ViewModel.GeoUiState

@Composable
fun AlgorithmSettingsRoute(
    onNavigateBack: () -> Unit
) {
    val viewModel: AlgorithmPreferencesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val geoState by viewModel.geoState.collectAsState()

    AlgorithmSettingsScreen(
        uiState = uiState,
        geoState = geoState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmSettingsScreen(
    uiState: AlgorithmWeightsUiState,
    geoState: GeoUiState,
    onEvent: (AlgorithmPreferencesEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is AlgorithmWeightsUiState.Content && uiState.errorMessage != null) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_LONG).show()
            onEvent(AlgorithmPreferencesEvent.ErrorShown)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed Algorithm Engine") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Personalize your algorithmic 'For You' feed. Adjust the weights below to steer what types of articles are boosted or penalized.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RegionLanguageSection(
                geoState = geoState,
                onRegionSelected = { onEvent(AlgorithmPreferencesEvent.SetRegion(it)) },
                onLanguagesChanged = { onEvent(AlgorithmPreferencesEvent.SetLanguages(it)) },
                onResetGeo = { onEvent(AlgorithmPreferencesEvent.ResetGeo) }
            )

            when (val state = uiState) {
                is AlgorithmWeightsUiState.Loading -> {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is AlgorithmWeightsUiState.Content -> {
                    AlgorithmTopicSlider(
                        title = "Technology & AI",
                        value = state.tech,
                        onValueChange = { onEvent(AlgorithmPreferencesEvent.UpdateWeights(it, state.politics, state.global, state.business, state.health)) }
                    )
                    AlgorithmTopicSlider(
                        title = "Politics & Government",
                        value = state.politics,
                        onValueChange = { onEvent(AlgorithmPreferencesEvent.UpdateWeights(state.tech, it, state.global, state.business, state.health)) }
                    )
                    AlgorithmTopicSlider(
                        title = "Business & Economy",
                        value = state.business,
                        onValueChange = { onEvent(AlgorithmPreferencesEvent.UpdateWeights(state.tech, state.politics, state.global, it, state.health)) }
                    )
                    AlgorithmTopicSlider(
                        title = "Global News",
                        value = state.global,
                        onValueChange = { onEvent(AlgorithmPreferencesEvent.UpdateWeights(state.tech, state.politics, it, state.business, state.health)) }
                    )
                    AlgorithmTopicSlider(
                        title = "Health & Wellness",
                        value = state.health,
                        onValueChange = { onEvent(AlgorithmPreferencesEvent.UpdateWeights(state.tech, state.politics, state.global, state.business, it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onEvent(AlgorithmPreferencesEvent.SaveAndRecalculate)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save & Re-Score Feed")
            }
        }
    }
}

// A small curated set of common regions, enough to cover the app's main audiences without a full
// ISO-3166 picker. The currently-selected region is always shown even if it isn't in this list.
private val COMMON_REGIONS = listOf(
    "US", "GB", "IN", "CA", "AU", "DE", "FR", "BR", "JP", "SG", "AE", "ZA"
)

// A curated short list of languages (ISO 639-1 → display label) for the manual override chips.
// Languages auto-resolved from the region but absent here stay selected silently — "Reset to
// auto-detect" is the escape hatch to clear a manual override entirely.
private val COMMON_LANGUAGES = listOf(
    "en" to "English", "es" to "Español", "fr" to "Français", "de" to "Deutsch",
    "pt" to "Português", "it" to "Italiano", "ar" to "العربية", "hi" to "हिन्दी",
    "zh" to "中文", "ja" to "日本語", "ru" to "Русский"
)

@Composable
private fun RegionLanguageSection(
    geoState: GeoUiState,
    onRegionSelected: (String) -> Unit,
    onLanguagesChanged: (List<String>) -> Unit,
    onResetGeo: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = geoState.homeRegion
    val options = remember(current) {
        (listOfNotNull(current) + COMMON_REGIONS).distinct()
    }
    val isOverridden = geoState.isManualOverride || geoState.languagesIsManualOverride

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Region & Language",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Stories from your region get a gentle boost and we show news in languages you read. We auto-detect both; override them if you prefer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(
                        text = current?.let { "Region: $it" } ?: "Region: Auto",
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                expanded = false
                                onRegionSelected(code)
                            }
                        )
                    }
                }
            }

            if (isOverridden) {
                TextButton(onClick = onResetGeo) {
                    Text("Reset to auto-detect")
                }
            }
        }

        LanguageChips(
            selected = geoState.readableLanguages,
            onLanguagesChanged = onLanguagesChanged
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageChips(
    selected: List<String>,
    onLanguagesChanged: (List<String>) -> Unit
) {
    val selectedSet = selected.toSet()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        COMMON_LANGUAGES.forEach { (code, label) ->
            val isSelected = code in selectedSet
            FilterChip(
                selected = isSelected,
                onClick = {
                    // Toggle off the last readable language too — sending an empty set drops the
                    // languages param entirely, which the backend reads as its neutral fallback.
                    val next = if (isSelected) selectedSet - code else selectedSet + code
                    onLanguagesChanged(next.toList())
                },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
            )
        }
    }
}

@Composable
private fun AlgorithmTopicSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "${(value * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 9
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Mute", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Boost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
