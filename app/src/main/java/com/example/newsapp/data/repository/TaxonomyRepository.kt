package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.newsapp.Hilt.TaxonomyDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaxonomyRepository @Inject constructor(
    @TaxonomyDataStore private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) {
    companion object {
        val KEY_TAXONOMY_JSON = stringPreferencesKey("taxonomy_json")
        val KEY_TAXONOMY_VERSION = stringPreferencesKey("taxonomy_version")

        // The Day 0 Fallback dictionary
        private val FALLBACK_DICTIONARY = mapOf(
            "tech" to listOf("tech", "technology", "software", "ai", "apple", "google", "microsoft", "cyber", "artificial intelligence", "machine learning"),
            "politics" to listOf("politics", "government", "election", "president", "congress", "senate", "policy", "supreme court"),
            "business" to listOf("economy", "stock", "market", "finance", "business", "inflation", "corporate", "wall street", "stock market", "federal reserve"),
            "sports" to listOf("sports", "football", "basketball", "soccer", "nfl", "nba", "championship", "athlete", "premier league"),
            "entertainment" to listOf("movie", "music", "hollywood", "celebrity", "entertainment", "actor", "award", "box office")
        )
    }

    /**
     * Flow that emits the dictionary. If the DataStore is empty, it returns the Day 0 fallback.
     */
    val dictionaryFlow: Flow<Map<String, List<String>>> = dataStore.data.map { prefs ->
        val jsonString = prefs[KEY_TAXONOMY_JSON]
        if (jsonString != null) {
            try {
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                gson.fromJson(jsonString, type)
            } catch (e: Exception) {
                FALLBACK_DICTIONARY
            }
        } else {
            FALLBACK_DICTIONARY
        }
    }

    suspend fun getVersion(): String {
        return dataStore.data.map { it[KEY_TAXONOMY_VERSION] ?: "0" }.firstOrNull() ?: "0"
    }

    suspend fun saveTaxonomy(version: String, categories: Map<String, List<String>>) {
        val jsonString = gson.toJson(categories)
        dataStore.edit { prefs ->
            prefs[KEY_TAXONOMY_VERSION] = version
            prefs[KEY_TAXONOMY_JSON] = jsonString
        }
    }
}
