package com.example.newsapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface PrivacyPreferencesRepository {
    val telemetryConsent: Flow<Boolean?>
    suspend fun setConsent(granted: Boolean)
}
