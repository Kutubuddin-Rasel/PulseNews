package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.newsapp.Hilt.PrivacyDataStore
import javax.inject.Inject
import javax.inject.Singleton

import com.example.newsapp.domain.repository.PrivacyPreferencesRepository

@Singleton
class PrivacyPreferencesRepositoryImpl @Inject constructor(
    @PrivacyDataStore private val dataStore: DataStore<Preferences>
) : PrivacyPreferencesRepository {
    companion object {
        val KEY_TELEMETRY_CONSENT = booleanPreferencesKey("telemetry_consent_granted")
    }

    override val telemetryConsent: Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[KEY_TELEMETRY_CONSENT]
    }

    override suspend fun setConsent(granted: Boolean) {
        withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs[KEY_TELEMETRY_CONSENT] = granted
        }
            Unit
        }
    }
}
