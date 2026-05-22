package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.data.repository.AlgorithmPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlgorithmWeightsUiState(
    val tech: Float = 0.2f,
    val politics: Float = 0.2f,
    val global: Float = 0.2f,
    val business: Float = 0.2f,
    val health: Float = 0.2f
)

@HiltViewModel
class AlgorithmPreferencesViewModel @Inject constructor(
    private val repository: AlgorithmPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlgorithmWeightsUiState())
    val uiState: StateFlow<AlgorithmWeightsUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.preferences.collectLatest { prefs ->
                _uiState.value = AlgorithmWeightsUiState(
                    tech = prefs["technology"] ?: 0.2f,
                    politics = prefs["politics"] ?: 0.2f,
                    global = prefs["general"] ?: 0.2f,
                    business = prefs["business"] ?: 0.2f,
                    health = prefs["health"] ?: 0.2f
                )
            }
        }
    }

    fun updateWeights(tech: Float, politics: Float, global: Float, business: Float, health: Float) {
        val total = tech + politics + global + business + health
        if (total == 0f) return
        
        val normTech = tech / total
        val normPolitics = politics / total
        val normGlobal = global / total
        val normBusiness = business / total
        val normHealth = health / total
        
        viewModelScope.launch {
            repository.updatePreferences(normTech, normPolitics, normGlobal, normBusiness, normHealth)
        }
    }
}
