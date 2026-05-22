package com.example.newsapp.Screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.newsapp.ViewModel.AlgorithmPreferencesViewModel
import com.example.newsapp.ui.components.NewsBackground
import com.example.newsapp.ui.components.enterpriseTopBarSpacing
import com.example.newsapp.ui.tokens.NewsSpacing

@Composable
fun AlgorithmPreferencesScreen() {
    val viewModel: AlgorithmPreferencesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    NewsBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .enterpriseTopBarSpacing()
                .padding(NewsSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(NewsSpacing.md)
        ) {
            Text(
                text = "Feed Algorithm",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            
            Text(
                text = "Customize the proportion of topics you want to see in your 'Everything' feed. The weights will automatically normalize to 100%.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA6D8CD)
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
        }
    }
}

@Composable
fun TopicSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text(
            text = "$label (${(value * 100).toInt()}%)",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}
