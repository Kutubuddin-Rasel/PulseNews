package com.example.newsapp.domain.model

data class GamificationProfile(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalArticlesRead: Int = 0,
    val lastReadDateEpochDay: Long = 0,
    val categoryReadCounts: Map<String, Int> = emptyMap(),
    val lastSyncedAt: Long = 0
) {
    // Helper to calculate total count from specific category
    fun getCategoryCount(category: String): Int {
        return categoryReadCounts[category.lowercase()] ?: 0
    }
}
