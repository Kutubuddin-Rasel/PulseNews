package com.example.newsapp.Screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.repository.AlgorithmPreferencesRepository
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.data.repository.NewsRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.newsapp.worker.ScoreRecalculationWorker

@HiltViewModel
class AlgorithmSettingsViewModel @Inject constructor(
    private val algoPrefsRepo: AlgorithmPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _preferences = mutableStateMapOf<String, Float>()
    val preferences: Map<String, Float> get() = _preferences

    init {
        viewModelScope.launch {
            val initialPrefs = algoPrefsRepo.preferences.first()
            listOf("technology", "politics", "business", "general", "health").forEach { topic ->
                _preferences[topic] = initialPrefs[topic] ?: 0.5f
            }
        }
    }

    fun updatePreference(topic: String, weight: Float) {
        _preferences[topic] = weight
    }

    fun saveAndRecalculate() {
        viewModelScope.launch {
            algoPrefsRepo.updatePreferences(
                tech = _preferences["technology"] ?: 0.5f,
                politics = _preferences["politics"] ?: 0.5f,
                global = _preferences["general"] ?: 0.5f,
                business = _preferences["business"] ?: 0.5f,
                health = _preferences["health"] ?: 0.5f
            )
            // Enqueue a WorkManager task to recalculate scores in the background (O(1) memory, bulk SQL)
            val workRequest = OneTimeWorkRequestBuilder<ScoreRecalculationWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmSettingsScreen(
    viewModel: AlgorithmSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

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

            // Dynamic sliders for each topic
            val topics = listOf(
                "technology" to "Technology & AI",
                "politics" to "Politics & Government",
                "business" to "Business & Economy",
                "general" to "Global News",
                "health" to "Health & Wellness"
            )

            topics.forEach { (key, title) ->
                TopicSlider(
                    title = title,
                    value = viewModel.preferences[key] ?: 0.5f,
                    onValueChange = { newValue ->
                        viewModel.updatePreference(key, newValue)
                    }
                )
            }

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
private fun TopicSlider(
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
            val percentage = (value * 100).toInt()
            Text(text = "$percentage%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 9 // Allows 10%, 20%, etc.
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
