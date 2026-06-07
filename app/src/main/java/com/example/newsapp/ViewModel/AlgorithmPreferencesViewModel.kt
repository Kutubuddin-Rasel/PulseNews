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
import android.content.Context
import java.util.ArrayDeque
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import com.example.newsapp.domain.model.UiEvent


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

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    private var updateJob: kotlinx.coroutines.Job? = null
    
    // Rate Limiting (5 requests per 5 minutes)
    private val requestTimestamps = ArrayDeque<Long>()
    private val MAX_REQUESTS = 5
    private val TIME_WINDOW_MS = 5 * 60 * 1000L
    
    // Debounce
    private val DEBOUNCE_MS = 2000L

    fun updateWeights(tech: Float, politics: Float, global: Float, business: Float, health: Float) {
        val total = tech + politics + global + business + health
        if (total == 0f) return
        
        val normTech = tech / total
        val normPolitics = politics / total
        val normGlobal = global / total
        val normBusiness = business / total
        val normHealth = health / total
        
        // Update local UI state immediately so slider feels responsive
        _uiState.value = AlgorithmWeightsUiState(normTech, normPolitics, normGlobal, normBusiness, normHealth)

        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            
            val now = System.currentTimeMillis()
            while (requestTimestamps.isNotEmpty() && (now - requestTimestamps.first()) > TIME_WINDOW_MS) {
                requestTimestamps.removeFirst()
            }
            
            if (requestTimestamps.size >= MAX_REQUESTS) {
                _events.emit(UiEvent.Generic("Rate limit exceeded. Please wait before adjusting algorithms again."))
                return@launch
            }
            
            requestTimestamps.addLast(now)
            repository.updatePreferences(normTech, normPolitics, normGlobal, normBusiness, normHealth)
        }
    }

    fun saveAndRecalculate() {
        // Pending decision: this screen is obsolete as backend handles sorting.
        // For now, it just saves the preferences to the repository.
    }
}
