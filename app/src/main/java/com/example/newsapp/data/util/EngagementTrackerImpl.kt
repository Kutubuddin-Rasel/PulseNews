package com.example.newsapp.data.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.newsapp.Hilt.GamificationDataStore
import com.example.newsapp.domain.model.GamificationProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.example.newsapp.domain.manager.FirestoreSyncManager
import com.example.newsapp.domain.tracker.EngagementTracker

@Singleton
class EngagementTrackerImpl @Inject constructor(
    @GamificationDataStore private val dataStore: DataStore<Preferences>,
    private val firestoreSyncManager: FirestoreSyncManager,
    moshi: Moshi
) : EngagementTracker {
    private val categoryCountsAdapter = moshi.adapter<Map<String, Int>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
    )

    companion object {
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val TOTAL_ARTICLES = intPreferencesKey("total_articles")
        val LAST_READ_DATE = longPreferencesKey("last_read_date_epoch_day")
        val CATEGORY_COUNTS = stringPreferencesKey("category_counts")
        val LAST_SYNCED = longPreferencesKey("last_synced")
    }

    override val profile: Flow<GamificationProfile> = dataStore.data.map { prefs ->
        val categoryJson = prefs[CATEGORY_COUNTS] ?: "{}"
        val categoryCounts: Map<String, Int> = categoryCountsAdapter.fromJson(categoryJson) ?: emptyMap()

        GamificationProfile(
            currentStreak = prefs[CURRENT_STREAK] ?: 0,
            longestStreak = prefs[LONGEST_STREAK] ?: 0,
            totalArticlesRead = prefs[TOTAL_ARTICLES] ?: 0,
            lastReadDateEpochDay = prefs[LAST_READ_DATE] ?: 0L,
            categoryReadCounts = categoryCounts,
            lastSyncedAt = prefs[LAST_SYNCED] ?: 0L
        )
    }

    override suspend fun recordArticleRead(category: String) {
        withContext(Dispatchers.IO) {
        // Un-cheatable UTC-to-Local logic
        // Get absolute UTC time
        val nowUtc = Instant.now()
        // Convert to user's *current* local timezone offset to get the correct calendar day
        val localDate = ZonedDateTime.ofInstant(nowUtc, ZoneId.systemDefault()).toLocalDate()
        val todayEpochDay = localDate.toEpochDay()

        var updatedProfile: GamificationProfile? = null

        dataStore.edit { prefs ->
            val lastRead = prefs[LAST_READ_DATE] ?: 0L
            var currentStreak = prefs[CURRENT_STREAK] ?: 0
            var longestStreak = prefs[LONGEST_STREAK] ?: 0
            var totalArticles = prefs[TOTAL_ARTICLES] ?: 0

            // Streak Logic
            if (lastRead == todayEpochDay) {
                // Already read today, do nothing to streak
            } else if (lastRead == todayEpochDay - 1) {
                // Consecutive day
                currentStreak++
                if (currentStreak > longestStreak) longestStreak = currentStreak
            } else {
                // Streak broken or first read
                currentStreak = 1
                if (currentStreak > longestStreak) longestStreak = currentStreak
            }

            prefs[LAST_READ_DATE] = todayEpochDay
            prefs[CURRENT_STREAK] = currentStreak
            prefs[LONGEST_STREAK] = longestStreak
            
            totalArticles++
            prefs[TOTAL_ARTICLES] = totalArticles

            // Category tracking
            val categoryJson = prefs[CATEGORY_COUNTS] ?: "{}"
            val categoryCounts: MutableMap<String, Int> =
                (categoryCountsAdapter.fromJson(categoryJson) ?: emptyMap()).toMutableMap()

            val safeCategory = category.lowercase().trim()
            categoryCounts[safeCategory] = (categoryCounts[safeCategory] ?: 0) + 1
            prefs[CATEGORY_COUNTS] = categoryCountsAdapter.toJson(categoryCounts)

            updatedProfile = GamificationProfile(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                totalArticlesRead = totalArticles,
                lastReadDateEpochDay = todayEpochDay,
                categoryReadCounts = categoryCounts,
                lastSyncedAt = prefs[LAST_SYNCED] ?: 0L
            )
        }

        // Fire sync event to Firestore
        updatedProfile?.let {
            firestoreSyncManager.pushGamificationState(it)
        }
            Unit
        }
    }

    override suspend fun updateFromRemote(remoteProfile: GamificationProfile) {
        withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            val localLastSync = prefs[LAST_SYNCED] ?: 0L
            
            // Basic conflict resolution: Remote wins if newer (which is expected on fresh install)
            if (remoteProfile.lastSyncedAt > localLastSync) {
                prefs[CURRENT_STREAK] = remoteProfile.currentStreak
                prefs[LONGEST_STREAK] = remoteProfile.longestStreak
                prefs[TOTAL_ARTICLES] = remoteProfile.totalArticlesRead
                prefs[LAST_READ_DATE] = remoteProfile.lastReadDateEpochDay
                prefs[CATEGORY_COUNTS] = categoryCountsAdapter.toJson(remoteProfile.categoryReadCounts)
                prefs[LAST_SYNCED] = remoteProfile.lastSyncedAt
            }
        }
            Unit
        }
    }
}
