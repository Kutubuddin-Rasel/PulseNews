package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.newsapp.Hilt.GeoLanguageDataStore
import com.example.newsapp.domain.model.GeoLanguageState
import com.example.newsapp.domain.repository.GeoLanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GeoLanguageRepositoryImpl @Inject constructor(
    @GeoLanguageDataStore private val dataStore: DataStore<Preferences>,
) : GeoLanguageRepository {

    private object Keys {
        val REGION = stringPreferencesKey("home_region")
        val LANGS = stringPreferencesKey("readable_languages") // CSV
        val REGION_OVERRIDE = booleanPreferencesKey("region_override")
        val LANGS_OVERRIDE = booleanPreferencesKey("langs_override")
    }

    override val state: Flow<GeoLanguageState> = dataStore.data.map { p ->
        GeoLanguageState(
            homeRegion = p[Keys.REGION],
            readableLanguages =
                p[Keys.LANGS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
            regionIsManualOverride = p[Keys.REGION_OVERRIDE] ?: false,
            languagesIsManualOverride = p[Keys.LANGS_OVERRIDE] ?: false,
        )
    }

    override suspend fun applyDetected(region: String?, languages: List<String>) {
        dataStore.edit { p ->
            if (p[Keys.REGION_OVERRIDE] != true && region != null) {
                p[Keys.REGION] = region
            }
            if (p[Keys.LANGS_OVERRIDE] != true && languages.isNotEmpty()) {
                p[Keys.LANGS] = languages.joinToString(",")
            }
        }
    }

    override suspend fun setManualRegion(region: String?) {
        dataStore.edit { p ->
            if (region == null) p.remove(Keys.REGION) else p[Keys.REGION] = region
            p[Keys.REGION_OVERRIDE] = true
        }
    }

    override suspend fun setManualLanguages(langs: List<String>) {
        dataStore.edit { p ->
            p[Keys.LANGS] = langs.joinToString(",")
            p[Keys.LANGS_OVERRIDE] = true
        }
    }

    override suspend fun resetToAuto() {
        dataStore.edit { p ->
            p[Keys.REGION_OVERRIDE] = false
            p[Keys.LANGS_OVERRIDE] = false
        }
    }
}
