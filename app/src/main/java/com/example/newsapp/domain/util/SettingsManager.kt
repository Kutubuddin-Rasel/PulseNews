package com.example.newsapp.domain.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

sealed interface ThemePreference {
    val name: String

    data object SYSTEM : ThemePreference { override val name = "SYSTEM" }
    data object LIGHT : ThemePreference { override val name = "LIGHT" }
    data object DARK : ThemePreference { override val name = "DARK" }

    companion object {
        fun valueOf(value: String): ThemePreference = when (value) {
            "SYSTEM" -> SYSTEM
            "LIGHT" -> LIGHT
            "DARK" -> DARK
            else -> throw IllegalArgumentException("No object com.example.newsapp.domain.util.ThemePreference.$value")
        }
    }
}

@Singleton
class SettingsManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val HIGH_CONTRAST_KEY = booleanPreferencesKey("high_contrast_enabled")
        val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
    }

    val highContrastEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HIGH_CONTRAST_KEY] ?: false
        }

    suspend fun setHighContrastEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIGH_CONTRAST_KEY] = enabled
        }
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data
        .map { preferences ->
            val themeStr = preferences[THEME_PREFERENCE_KEY] ?: ThemePreference.SYSTEM.name
            runCatching { ThemePreference.valueOf(themeStr) }.getOrDefault(ThemePreference.SYSTEM)
        }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = preference.name
        }
    }
}
