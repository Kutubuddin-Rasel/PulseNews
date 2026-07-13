package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.newsapp.Hilt.EngagementDataStore
import com.example.newsapp.domain.repository.RecentSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecentSearchRepositoryImpl @Inject constructor(
    @EngagementDataStore private val dataStore: DataStore<Preferences>
) : RecentSearchRepository {

    override fun getRecentSearches(): Flow<List<String>> {
        return dataStore.data.map { prefs ->
            prefs[RECENT_SEARCHES_KEY]
                ?.split(RECENT_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
    }

    override suspend fun recordRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_RECENT_LENGTH) return
        dataStore.edit { prefs ->
            val current = prefs[RECENT_SEARCHES_KEY]
                ?.split(RECENT_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val deduped = current.filterNot { it.equals(trimmed, ignoreCase = true) }
            val updated = (listOf(trimmed) + deduped).take(MAX_RECENT_SEARCHES)
            prefs[RECENT_SEARCHES_KEY] = updated.joinToString(RECENT_SEPARATOR)
        }
    }

    override suspend fun clearRecentSearches() {
        dataStore.edit { prefs -> prefs.remove(RECENT_SEARCHES_KEY) }
    }

    companion object {
        private const val MAX_RECENT_SEARCHES = 8
        private const val MIN_RECENT_LENGTH = 2
        private const val RECENT_SEPARATOR = "\u0001" // SOH — never appears in user queries
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
    }
}
