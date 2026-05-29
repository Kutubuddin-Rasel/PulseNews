package com.example.newsapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsapp.domain.util.SettingsManager
import com.example.newsapp.domain.util.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val highContrastEnabled: StateFlow<Boolean> = settingsManager.highContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val themePreference: StateFlow<ThemePreference> = settingsManager.themePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreference.SYSTEM
        )

    fun toggleHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setHighContrastEnabled(enabled)
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch {
            settingsManager.setThemePreference(preference)
        }
    }
}
