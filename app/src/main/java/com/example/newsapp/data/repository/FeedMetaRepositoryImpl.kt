package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.newsapp.Hilt.FeedMetaDataStore
import com.example.newsapp.data.remote.dto.PulseMetaDto
import com.example.newsapp.domain.repository.FeedMetaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedMetaRepositoryImpl @Inject constructor(
    @FeedMetaDataStore private val dataStore: DataStore<Preferences>
) : FeedMetaRepository {

    private object PreferencesKeys {
        val TOTAL_PAGES = intPreferencesKey("total_pages")
        val LAST_UPDATED = stringPreferencesKey("last_updated")
        val FETCHED_AT_MS = longPreferencesKey("meta_fetched_at_ms")
    }

    override val meta: Flow<PulseMetaDto?> = dataStore.data.map { preferences ->
        val totalPages = preferences[PreferencesKeys.TOTAL_PAGES]
        val lastUpdated = preferences[PreferencesKeys.LAST_UPDATED]

        if (totalPages != null && lastUpdated != null) {
            PulseMetaDto(totalPages = totalPages, lastUpdated = lastUpdated)
        } else {
            null
        }
    }

    override val metaFetchedAtMs: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FETCHED_AT_MS]
    }

    override suspend fun saveMeta(totalPages: Int, lastUpdated: String, fetchedAtMs: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_PAGES] = totalPages
            preferences[PreferencesKeys.LAST_UPDATED] = lastUpdated
            preferences[PreferencesKeys.FETCHED_AT_MS] = fetchedAtMs
        }
    }
}
