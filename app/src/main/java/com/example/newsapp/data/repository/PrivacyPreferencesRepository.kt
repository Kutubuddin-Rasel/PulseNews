package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.newsapp.Hilt.PrivacyDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyPreferencesRepository @Inject constructor(
    @PrivacyDataStore private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_TELEMETRY_CONSENT = booleanPreferencesKey("telemetry_consent_granted")
    }

    val telemetryConsent: Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[KEY_TELEMETRY_CONSENT]
    }

    suspend fun setConsent(granted: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_TELEMETRY_CONSENT] = granted
        }
    }
}
