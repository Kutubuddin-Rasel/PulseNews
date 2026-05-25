package com.example.newsapp.data.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.newsapp.Hilt.EngagementDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalEngagementTracker @Inject constructor(
    @EngagementDataStore private val dataStore: DataStore<Preferences>
) {
    fun trackClick(categoryId: Int) {
        // Increment the counter for this category
    }

    suspend fun incrementClick(categoryId: Int) {
        val key = intPreferencesKey("clicks_category_$categoryId")
        dataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            prefs[key] = current + 1
        }
    }

    fun getCohortDistribution(): Flow<Map<Int, Float>> {
        return dataStore.data.map { prefs ->
            val clicks = mutableMapOf<Int, Int>()
            var totalClicks = 0
            
            // We assume categories 1 to 7 based on our app's defined categories
            for (i in 1..7) {
                val count = prefs[intPreferencesKey("clicks_category_$i")] ?: 0
                clicks[i] = count
                totalClicks += count
            }
            
            val distribution = mutableMapOf<Int, Float>()
            if (totalClicks > 0) {
                for ((cat, count) in clicks) {
                    distribution[cat] = count.toFloat() / totalClicks.toFloat()
                }
            } else {
                // Default even distribution if no clicks
                for (i in 1..7) {
                    distribution[i] = 1f / 7f
                }
            }
            distribution
        }
    }
}
