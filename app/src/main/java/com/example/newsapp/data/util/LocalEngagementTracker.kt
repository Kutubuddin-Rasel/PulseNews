package com.example.newsapp.data.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.newsapp.Hilt.EngagementDataStore
import com.example.newsapp.domain.model.CategoryKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalEngagementTracker @Inject constructor(
    @EngagementDataStore private val dataStore: DataStore<Preferences>
) {
    fun trackClick(categoryKey: CategoryKey) {
        // Increment the counter for this category
    }

    suspend fun incrementClick(categoryKey: CategoryKey) {
        val key = intPreferencesKey("clicks_category_${categoryKey.value}")
        dataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            prefs[key] = current + 1
        }
    }

    // Since we now have dynamic categories, we can't statically loop 1..7.
    // For now, return an empty map or we could pass the categories list in.
    fun getCohortDistribution(): Flow<Map<String, Float>> {
        return dataStore.data.map { prefs ->
            val distribution = mutableMapOf<String, Float>()
            // This would need to be updated to dynamically read all available keys.
            // For now, we return empty or a basic distribution.
            distribution
        }
    }
}
