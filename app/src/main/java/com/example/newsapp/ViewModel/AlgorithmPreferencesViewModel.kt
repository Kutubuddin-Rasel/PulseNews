package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.Hilt.ApplicationScope
import com.example.newsapp.domain.repository.AlgorithmPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.ArrayDeque
import kotlinx.coroutines.delay

sealed interface AlgorithmWeightsUiState {
    data object Loading : AlgorithmWeightsUiState
    data class Content(
        val tech: Float = 0.2f,
        val politics: Float = 0.2f,
        val global: Float = 0.2f,
        val business: Float = 0.2f,
        val health: Float = 0.2f,
        val errorMessage: String? = null
    ) : AlgorithmWeightsUiState
}

sealed interface AlgorithmPreferencesEvent {
    data class UpdateWeights(
        val tech: Float,
        val politics: Float,
        val global: Float,
        val business: Float,
        val health: Float
    ) : AlgorithmPreferencesEvent
    data object SaveAndRecalculate : AlgorithmPreferencesEvent
    data object ErrorShown : AlgorithmPreferencesEvent
}

@HiltViewModel
class AlgorithmPreferencesViewModel @Inject constructor(
    private val repository: AlgorithmPreferencesRepository,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlgorithmWeightsUiState>(AlgorithmWeightsUiState.Loading)
    val uiState: StateFlow<AlgorithmWeightsUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.preferences.collectLatest { prefs ->
                _uiState.value = AlgorithmWeightsUiState.Content(
                    tech = prefs["technology"] ?: 0.2f,
                    politics = prefs["politics"] ?: 0.2f,
                    global = prefs["general"] ?: 0.2f,
                    business = prefs["business"] ?: 0.2f,
                    health = prefs["health"] ?: 0.2f
                )
            }
        }
    }

    private var updateJob: kotlinx.coroutines.Job? = null
    
    // Rate Limiting (5 requests per 5 minutes)
    private val requestTimestamps = ArrayDeque<Long>()
    private val MAX_REQUESTS = 5
    private val TIME_WINDOW_MS = 5 * 60 * 1000L
    
    // Debounce
    private val DEBOUNCE_MS = 2000L

    fun onEvent(event: AlgorithmPreferencesEvent) {
        when (event) {
            is AlgorithmPreferencesEvent.UpdateWeights -> {
                updateWeights(event.tech, event.politics, event.global, event.business, event.health)
            }
            is AlgorithmPreferencesEvent.SaveAndRecalculate -> {
                saveAndRecalculate()
            }
            is AlgorithmPreferencesEvent.ErrorShown -> {
                val currentState = _uiState.value
                if (currentState is AlgorithmWeightsUiState.Content) {
                    _uiState.value = currentState.copy(errorMessage = null)
                }
            }
        }
    }

    private fun updateWeights(tech: Float, politics: Float, global: Float, business: Float, health: Float) {
        val total = tech + politics + global + business + health
        if (total == 0f) return
        
        val normTech = tech / total
        val normPolitics = politics / total
        val normGlobal = global / total
        val normBusiness = business / total
        val normHealth = health / total
        
        val currentState = _uiState.value
        val errorMsg = if (currentState is AlgorithmWeightsUiState.Content) currentState.errorMessage else null
        
        // Update local UI state immediately so slider feels responsive
        _uiState.value = AlgorithmWeightsUiState.Content(
            tech = normTech, 
            politics = normPolitics, 
            global = normGlobal, 
            business = normBusiness, 
            health = normHealth,
            errorMessage = errorMsg
        )

        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            
            val now = System.currentTimeMillis()
            while (requestTimestamps.isNotEmpty() && (now - requestTimestamps.first()) > TIME_WINDOW_MS) {
                requestTimestamps.removeFirst()
            }
            
            if (requestTimestamps.size >= MAX_REQUESTS) {
                val state = _uiState.value
                if (state is AlgorithmWeightsUiState.Content) {
                    _uiState.value = state.copy(errorMessage = "Rate limit exceeded. Please wait before adjusting algorithms again.")
                }
                return@launch
            }
            
            requestTimestamps.addLast(now)
            repository.updatePreferences(normTech, normPolitics, normGlobal, normBusiness, normHealth)
        }
    }

    private fun saveAndRecalculate() {
        // ALG2: sliders persist on a 2s debounce, so tapping "Save & Re-Score Feed" and navigating
        // back within that window would otherwise drop the last edit when this ViewModel (and its
        // viewModelScope) is cleared. Cancel the pending debounce and flush the current — already
        // normalized — weights on the application scope so the write survives the screen going away.
        // The explicit Save bypasses the slider rate-limiter: it's one deliberate action, not the
        // rapid drag the limiter throttles (and it avoids racing on the shared timestamp deque).
        val state = _uiState.value
        if (state !is AlgorithmWeightsUiState.Content) return
        updateJob?.cancel()
        appScope.launch {
            repository.updatePreferences(state.tech, state.politics, state.global, state.business, state.health)
        }
    }
}
