package com.example.newsapp.domain.manager

import com.example.newsapp.module.Article
import com.example.newsapp.domain.model.GamificationProfile

interface FirestoreSyncManager {
    suspend fun pushArticleSave(article: Article)
    suspend fun pushArticleUnsave(url: String)
    suspend fun pushPreferences(tech: Float, politics: Float, global: Float, business: Float, health: Float)
    fun pushGamificationState(profile: GamificationProfile)
}
