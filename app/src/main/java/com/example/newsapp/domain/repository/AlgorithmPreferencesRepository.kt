package com.example.newsapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface AlgorithmPreferencesRepository {
    val preferences: Flow<Map<String, Float>>
    suspend fun updatePreferences(tech: Float, politics: Float, global: Float, business: Float, health: Float)
}
