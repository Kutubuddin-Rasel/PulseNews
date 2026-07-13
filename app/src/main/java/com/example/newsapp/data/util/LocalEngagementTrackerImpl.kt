package com.example.newsapp.data.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.newsapp.Hilt.EngagementDataStore
import com.example.newsapp.domain.model.CategoryKey
import javax.inject.Inject
import javax.inject.Singleton

import com.example.newsapp.domain.tracker.LocalEngagementTracker

@Singleton
class LocalEngagementTrackerImpl @Inject constructor(
    @EngagementDataStore private val dataStore: DataStore<Preferences>
) : LocalEngagementTracker {
    override fun trackClick(categoryKey: CategoryKey) {
        // Increment the counter for this category
    }

    override suspend fun incrementClick(categoryKey: CategoryKey) {
        withContext(Dispatchers.IO) {
        val key = intPreferencesKey("clicks_category_${categoryKey.value}")
        dataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            prefs[key] = current + 1
        }
            Unit
        }
    }

    // Since we now have dynamic categories, we can't statically loop 1..7.
    // For now, return an empty map or we could pass the categories list in.
    override fun getCohortDistribution(): Flow<Map<String, Float>> {
        return dataStore.data.map { prefs ->
            val distribution = mutableMapOf<String, Float>()
            // This would need to be updated to dynamically read all available keys.
            // For now, we return empty or a basic distribution.
            distribution
        }
    }
}
