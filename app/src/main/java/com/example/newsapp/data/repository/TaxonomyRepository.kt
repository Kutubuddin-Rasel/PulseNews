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
import com.example.newsapp.domain.model.CategoryKey
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
            "business" to listOf("economy", "stock", "market", "finance", "business", "inflation", "corporate", "wall street", "stock market", "federal reserve"),
            "sports" to listOf("sports", "football", "basketball", "soccer", "nfl", "nba", "championship", "athlete", "premier league"),
            "politics" to listOf("politics", "government", "election", "president", "congress", "senate", "policy", "supreme court"),
            "health" to listOf("health", "medicine", "wellness", "medical", "disease", "covid", "hospital", "fitness"),
            "science" to listOf("science", "space", "nasa", "research", "physics", "biology", "astronomy", "discovery"),
            "entertainment" to listOf("movie", "music", "hollywood", "celebrity", "entertainment", "actor", "award", "box office"),
            "world" to listOf("world", "global", "international", "europe", "asia", "africa", "un", "diplomacy"),
            "crypto" to listOf("crypto", "bitcoin", "ethereum", "blockchain", "web3", "nft", "cryptocurrency"),
            "design" to listOf("design", "ui", "ux", "architecture", "art", "creative", "fashion", "graphic design")
        )
    }

    /**
     * Flow that emits the dictionary. If the DataStore is empty, it returns the Day 0 fallback.
     */
    val dictionaryFlow: Flow<Map<CategoryKey, List<String>>> = dataStore.data.map { prefs ->
        val map = mutableMapOf<CategoryKey, List<String>>()
        val jsonString = prefs[KEY_TAXONOMY_JSON]
        if (jsonString != null) {
            try {
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                val parsed: Map<String, List<String>> = gson.fromJson(jsonString, type)
                parsed.forEach { (k, v) -> map[CategoryKey(k)] = v }
                map
            } catch (e: Exception) {
                FALLBACK_DICTIONARY.mapKeys { CategoryKey(it.key) }
            }
        } else {
            FALLBACK_DICTIONARY.mapKeys { CategoryKey(it.key) }
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
