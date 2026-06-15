package com.example.newsapp.data.util

import android.util.Log
import com.example.newsapp.Room.InteractionEventDao
import com.example.newsapp.Room.InteractionEventEntity
import com.example.newsapp.domain.repository.PrivacyPreferencesRepository
import com.example.newsapp.Hilt.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.example.newsapp.domain.util.AppTelemetry

@Singleton
class AppTelemetryImpl @Inject constructor(
    private val privacyPrefs: PrivacyPreferencesRepository,
    private val interactionEventDao: InteractionEventDao,
    @ApplicationScope private val appScope: CoroutineScope
) : AppTelemetry {
    // @Volatile: written on the IO collector, read from interceptor/UI threads (audit T2).
    @Volatile
    private var isConsentGranted: Boolean = false

    init {
        appScope.launch {
            privacyPrefs.telemetryConsent.collectLatest { consent ->
                isConsentGranted = consent == true
            }
        }
    }

    override fun requestId(): String = UUID.randomUUID().toString()

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
        syncRemote("INFO", tag, message)
    }

    override fun warn(tag: String, message: String) {
        Log.w(tag, message)
        syncRemote("WARN", tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        syncRemote("ERROR", tag, message, throwable)
    }

    private fun syncRemote(level: String, tag: String, message: String, throwable: Throwable? = null) {
        if (!isConsentGranted) return
        
        // Future integration point for Firebase Crashlytics / Analytics
        // Example: FirebaseCrashlytics.getInstance().log("$level: $tag: $message")
        // Example: if (throwable != null) FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    override fun trackClick(articleId: String) {
        insertEvent(articleId, "CLICK", com.google.gson.JsonObject())
    }

    override fun trackReadDeep(articleId: String, durationSeconds: Long, scrollDepthPercent: Int) {
        val data = com.google.gson.JsonObject().apply {
            addProperty("duration_seconds", durationSeconds)
            addProperty("scroll_depth_percent", scrollDepthPercent)
        }
        insertEvent(articleId, "READ_DEEP", data)
    }

    override fun trackBookmark(articleId: String) {
        insertEvent(articleId, "BOOKMARK", com.google.gson.JsonObject())
    }

    override fun trackShare(articleId: String, platform: String) {
        val data = com.google.gson.JsonObject().apply {
            addProperty("platform", platform)
        }
        insertEvent(articleId, "SHARE", data)
    }

    override fun trackBlockSource(articleId: String, sourceDomain: String) {
        val data = com.google.gson.JsonObject().apply {
            addProperty("sourceDomain", sourceDomain)
        }
        insertEvent(articleId, "BLOCK_SOURCE", data)
    }

    private fun insertEvent(articleId: String, interactionType: String, data: com.google.gson.JsonObject) {
        if (!isConsentGranted) return
        
        appScope.launch {
            val event = InteractionEventEntity(
                articleId = articleId,
                interactionType = interactionType,
                timestamp = System.currentTimeMillis(),
                eventData = data.toString()
            )
            interactionEventDao.insertEvent(event)
        }
    }
}
