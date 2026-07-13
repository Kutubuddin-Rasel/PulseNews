package com.example.newsapp.domain.tracker

import com.example.newsapp.domain.model.CategoryKey
import kotlinx.coroutines.flow.Flow

interface LocalEngagementTracker {
    fun trackClick(categoryKey: CategoryKey)
    suspend fun incrementClick(categoryKey: CategoryKey)
    fun getCohortDistribution(): Flow<Map<String, Float>>
}
