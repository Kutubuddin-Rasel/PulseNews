package com.example.newsapp.Screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ViewModel.AlgorithmPreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmPreferencesScreen(
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: AlgorithmPreferencesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed Algorithm") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Customize the proportion of topics you want to see in your 'For You' feed. Weights normalize automatically to 100%.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TopicSlider(
                label = "Technology",
                value = uiState.tech,
                onValueChange = { viewModel.updateWeights(it, uiState.politics, uiState.global, uiState.business, uiState.health) }
            )
            TopicSlider(
                label = "Politics",
                value = uiState.politics,
                onValueChange = { viewModel.updateWeights(uiState.tech, it, uiState.global, uiState.business, uiState.health) }
            )
            TopicSlider(
                label = "Global News",
                value = uiState.global,
                onValueChange = { viewModel.updateWeights(uiState.tech, uiState.politics, it, uiState.business, uiState.health) }
            )
            TopicSlider(
                label = "Business",
                value = uiState.business,
                onValueChange = { viewModel.updateWeights(uiState.tech, uiState.politics, uiState.global, it, uiState.health) }
            )
            TopicSlider(
                label = "Health",
                value = uiState.health,
                onValueChange = { viewModel.updateWeights(uiState.tech, uiState.politics, uiState.global, uiState.business, it) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveAndRecalculate()
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
private fun TopicSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}
