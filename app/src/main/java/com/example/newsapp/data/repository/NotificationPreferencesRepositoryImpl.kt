package com.example.newsapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

import com.example.newsapp.domain.repository.NotificationPreferencesRepository

val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

@Singleton
class NotificationPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationPreferencesRepository {
    private val dataStore = context.notificationDataStore

    companion object {
        val SUBSCRIBED_TOPICS = stringSetPreferencesKey("subscribed_topics")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START_MINUTES = intPreferencesKey("quiet_hours_start_minutes") // Minutes from midnight
        val QUIET_HOURS_END_MINUTES = intPreferencesKey("quiet_hours_end_minutes") // Minutes from midnight
        val MAX_DAILY_NOTIFICATIONS = intPreferencesKey("max_daily_notifications")
        val CURRENT_DAILY_COUNT = intPreferencesKey("current_daily_count")
        val LAST_RESET_DATE = longPreferencesKey("last_reset_date")
    }

    override val subscribedTopics: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SUBSCRIBED_TOPICS] ?: emptySet()
    }

    override val quietHoursEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_ENABLED] ?: false
    }

    override val quietHoursStartMinutes: Flow<Int> = dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_START_MINUTES] ?: (22 * 60) // Default 10 PM
    }

    override val quietHoursEndMinutes: Flow<Int> = dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_END_MINUTES] ?: (7 * 60) // Default 7 AM
    }

    override val maxDailyNotifications: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MAX_DAILY_NOTIFICATIONS] ?: 5 // Default 5 notifications
    }

    override val currentDailyCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[CURRENT_DAILY_COUNT] ?: 0
    }

    override val lastResetDate: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_RESET_DATE] ?: 0L
    }

    override suspend fun setTopics(topics: Set<String>) {
        withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[SUBSCRIBED_TOPICS] = topics
        }
            Unit
        }
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[QUIET_HOURS_ENABLED] = enabled
        }
            Unit
        }
    }

    override suspend fun setQuietHours(startMinutes: Int, endMinutes: Int) {
        withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[QUIET_HOURS_START_MINUTES] = startMinutes
            preferences[QUIET_HOURS_END_MINUTES] = endMinutes
        }
            Unit
        }
    }

    override suspend fun setMaxDailyNotifications(max: Int) {
        withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[MAX_DAILY_NOTIFICATIONS] = max
        }
            Unit
        }
    }

    override suspend fun incrementDailyCount() {
        withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            val current = preferences[CURRENT_DAILY_COUNT] ?: 0
            preferences[CURRENT_DAILY_COUNT] = current + 1
        }
            Unit
        }
    }

    override suspend fun resetDailyCountIfNeeded(currentDayMillis: Long) {
        withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[CURRENT_DAILY_COUNT] = 0
            preferences[LAST_RESET_DATE] = currentDayMillis
        }
            Unit
        }
    }
}
