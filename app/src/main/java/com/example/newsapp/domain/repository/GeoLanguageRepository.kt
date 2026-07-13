package com.example.newsapp.domain.repository

import com.example.newsapp.domain.model.GeoLanguageState
import kotlinx.coroutines.flow.Flow

interface GeoLanguageRepository {
    val state: Flow<GeoLanguageState>

    /** Apply auto-detected values, never overwriting a manually-overridden field. */
    suspend fun applyDetected(region: String?, languages: List<String>)

    suspend fun setManualRegion(region: String?)
    suspend fun setManualLanguages(langs: List<String>)

    /** Clear both override flags so the next applyDetected takes effect again. */
    suspend fun resetToAuto()
}
