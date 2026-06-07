package com.example.newsapp.data.util

import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.remote.dto.TelemetryBatchRequest
import com.example.newsapp.data.remote.dto.TelemetryEvent
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.ArrayDeque
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Singleton
class TelemetryManager @Inject constructor(
    private val pulseBackendApi: PulseBackendApi,
    private val connectivityMonitor: ConnectivityMonitor,
    private val clockProvider: ClockProvider
) {
    private val MAX_QUEUE_SIZE = 100
    private val queue = ArrayDeque<TelemetryEvent>(MAX_QUEUE_SIZE)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Deduplication tracking
    private var lastClickedArticleId: String? = null
    private var lastClickedTimeMs: Long = 0
    private val CLICK_DEDUPE_WINDOW_MS = 60_000L // 1 minute

    // Device ID handling - in a real app this should be persistent across app installs,
    // but for this implementation we generate one if needed or pull from preferences.
    // For now we'll just use a random UUID per session, but ideally it should come from a DataStore.
    private val deviceId = UUID.randomUUID().toString() 

    private fun addEvent(event: TelemetryEvent) {
        synchronized(queue) {
            if (queue.size >= MAX_QUEUE_SIZE) {
                // Ring buffer behavior: drop the oldest event
                queue.pollFirst()
            }
            queue.addLast(event)
            
            // Background flush if we hit 50 events
            if (queue.size >= 50) {
                flush()
            }
        }
    }

    private fun getCurrentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(clockProvider.nowMillis()))
    }

    fun trackClick(articleId: String, rankPosition: Int, source: String = "home_feed") {
        val now = clockProvider.nowMillis()
        
        // Deduplication
        if (articleId == lastClickedArticleId && (now - lastClickedTimeMs) < CLICK_DEDUPE_WINDOW_MS) {
            return // Drop duplicate click
        }
        
        lastClickedArticleId = articleId
        lastClickedTimeMs = now

        val data = JsonObject(mapOf(
            "source" to JsonPrimitive(source),
            "rank_position" to JsonPrimitive(rankPosition)
        ))

        addEvent(
            TelemetryEvent(
                type = "ARTICLE_CLICKED",
                articleId = articleId,
                timestamp = getCurrentIsoTime(),
                data = data
            )
        )
    }

    fun trackDwell(articleId: String, durationSeconds: Long, scrollDepthPercent: Int) {
        // Misclick filter
        if (durationSeconds < 2) {
            return // Discard noise
        }

        val data = JsonObject(mapOf(
            "duration_seconds" to JsonPrimitive(durationSeconds),
            "scroll_depth_percent" to JsonPrimitive(scrollDepthPercent)
        ))

        addEvent(
            TelemetryEvent(
                type = "DWELL_TIME",
                articleId = articleId,
                timestamp = getCurrentIsoTime(),
                data = data
            )
        )
    }

    fun trackShare(articleId: String, platform: String) {
        val data = JsonObject(mapOf(
            "platform" to JsonPrimitive(platform)
        ))

        addEvent(
            TelemetryEvent(
                type = "ARTICLE_SHARED",
                articleId = articleId,
                timestamp = getCurrentIsoTime(),
                data = data
            )
        )
    }

    fun trackSaved(articleId: String) {
        addEvent(
            TelemetryEvent(
                type = "ARTICLE_SAVED",
                articleId = articleId,
                timestamp = getCurrentIsoTime(),
                data = JsonObject(emptyMap())
            )
        )
    }

    fun flush() {
        if (!connectivityMonitor.isOnline()) return

        val eventsToSend = synchronized(queue) {
            if (queue.isEmpty()) return
            queue.toList()
        }

        coroutineScope.launch {
            try {
                val request = TelemetryBatchRequest(events = eventsToSend)
                val response = pulseBackendApi.postInteractions(
                    deviceId = deviceId,
                    request = request
                )
                
                if (response.isSuccessful) {
                    synchronized(queue) {
                        // Remove the successfully sent events
                        eventsToSend.forEach { queue.remove(it) }
                    }
                }
            } catch (e: Exception) {
                // If it fails, do nothing. The ring buffer will handle overflow safely.
            }
        }
    }
}
