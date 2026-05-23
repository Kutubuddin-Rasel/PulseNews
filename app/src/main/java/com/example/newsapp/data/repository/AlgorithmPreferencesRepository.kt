package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import com.example.newsapp.data.util.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class AlgorithmPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val firestoreSyncManager: FirestoreSyncManager
) {
    companion object {
        val KEY_TECH = floatPreferencesKey("algo_tech")
        val KEY_POLITICS = floatPreferencesKey("algo_politics")
        val KEY_GLOBAL = floatPreferencesKey("algo_global")
        val KEY_BUSINESS = floatPreferencesKey("algo_business")
        val KEY_HEALTH = floatPreferencesKey("algo_health")
    }

    val preferences: Flow<Map<String, Float>> = dataStore.data.map { prefs ->
        mapOf(
            "technology" to (prefs[KEY_TECH] ?: 0.2f),
            "politics" to (prefs[KEY_POLITICS] ?: 0.2f),
            "general" to (prefs[KEY_GLOBAL] ?: 0.2f),
            "business" to (prefs[KEY_BUSINESS] ?: 0.2f),
            "health" to (prefs[KEY_HEALTH] ?: 0.2f)
        )
    }

    suspend fun updatePreferences(tech: Float, politics: Float, global: Float, business: Float, health: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_TECH] = tech
            prefs[KEY_POLITICS] = politics
            prefs[KEY_GLOBAL] = global
            prefs[KEY_BUSINESS] = business
            prefs[KEY_HEALTH] = health
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            firestoreSyncManager.pushPreferences(tech, politics, global, business, health)
        }
    }
}
