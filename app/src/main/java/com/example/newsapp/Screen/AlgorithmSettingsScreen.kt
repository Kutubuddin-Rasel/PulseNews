package com.example.newsapp.Screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ViewModel.AlgorithmPreferencesViewModel
import com.example.newsapp.ViewModel.AlgorithmPreferencesEvent
import com.example.newsapp.ViewModel.AlgorithmWeightsUiState

@Composable
fun AlgorithmSettingsRoute(
    onNavigateBack: () -> Unit
) {
    val viewModel: AlgorithmPreferencesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    AlgorithmSettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmSettingsScreen(
    uiState: AlgorithmWeightsUiState,
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
