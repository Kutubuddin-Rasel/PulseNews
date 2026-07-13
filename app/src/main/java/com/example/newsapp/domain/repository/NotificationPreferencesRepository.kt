package com.example.newsapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface NotificationPreferencesRepository {
    val subscribedTopics: Flow<Set<String>>
    val quietHoursEnabled: Flow<Boolean>
    val quietHoursStartMinutes: Flow<Int>
    val quietHoursEndMinutes: Flow<Int>
    val maxDailyNotifications: Flow<Int>
    val currentDailyCount: Flow<Int>
    val lastResetDate: Flow<Long>

    suspend fun setTopics(topics: Set<String>)
    suspend fun setQuietHoursEnabled(enabled: Boolean)
    suspend fun setQuietHours(startMinutes: Int, endMinutes: Int)
    suspend fun setMaxDailyNotifications(max: Int)
    suspend fun incrementDailyCount()
    suspend fun resetDailyCountIfNeeded(currentDayMillis: Long)
}
