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
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

import com.example.newsapp.domain.util.AppTelemetry

@Singleton
class AppTelemetryImpl @Inject constructor(
    private val privacyPrefs: PrivacyPreferencesRepository,
    private val interactionEventDao: InteractionEventDao,
    @ApplicationScope private val appScope: CoroutineScope,
    moshi: Moshi
) : AppTelemetry {
    // Serializes each event's `data` payload (heterogeneous per type) to the JSON string stored in
    // Room. Concrete Long/Int values are written as integers, so the wire keeps `42` (not `42.0`).
    private val dataAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

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
        insertEvent(articleId, "CLICK", emptyMap())
    }

    override fun trackReadDeep(articleId: String, durationSeconds: Long, scrollDepthPercent: Int) {
        insertEvent(
            articleId,
            "READ_DEEP",
            mapOf(
                "duration_seconds" to durationSeconds,
                "scroll_depth_percent" to scrollDepthPercent
            )
        )
    }

    override fun trackBookmark(articleId: String) {
        insertEvent(articleId, "BOOKMARK", emptyMap())
    }

    override fun trackShare(articleId: String, platform: String) {
        insertEvent(articleId, "SHARE", mapOf("platform" to platform))
    }

    override fun trackBlockSource(articleId: String, sourceDomain: String) {
        insertEvent(articleId, "BLOCK_SOURCE", mapOf("sourceDomain" to sourceDomain))
    }

    private fun insertEvent(articleId: String, interactionType: String, data: Map<String, Any?>) {
        if (!isConsentGranted) return

        appScope.launch {
            val event = InteractionEventEntity(
                articleId = articleId,
                interactionType = interactionType,
                timestamp = System.currentTimeMillis(),
                eventData = dataAdapter.toJson(data)
            )
            interactionEventDao.insertEvent(event)
        }
    }
}
