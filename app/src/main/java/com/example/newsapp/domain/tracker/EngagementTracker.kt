package com.example.newsapp.domain.tracker

import com.example.newsapp.domain.model.GamificationProfile
import kotlinx.coroutines.flow.Flow

interface EngagementTracker {
    val profile: Flow<GamificationProfile>
    suspend fun recordArticleRead(category: String = "general")
    suspend fun updateFromRemote(remoteProfile: GamificationProfile)
}
