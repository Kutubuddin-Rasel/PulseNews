package com.example.newsapp.data.util

import android.util.Log
import com.example.newsapp.data.repository.PrivacyPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppTelemetry @Inject constructor(
    private val privacyPrefs: PrivacyPreferencesRepository
) {
    private var isConsentGranted: Boolean = false

    init {
        CoroutineScope(Dispatchers.IO).launch {
            privacyPrefs.telemetryConsent.collectLatest { consent ->
                isConsentGranted = consent == true
            }
        }
    }

    fun requestId(): String = UUID.randomUUID().toString()

    fun info(tag: String, message: String) {
        Log.i(tag, message)
        syncRemote("INFO", tag, message)
    }

    fun warn(tag: String, message: String) {
        Log.w(tag, message)
        syncRemote("WARN", tag, message)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        syncRemote("ERROR", tag, message, throwable)
    }

    private fun syncRemote(level: String, tag: String, message: String, throwable: Throwable? = null) {
        if (!isConsentGranted) return
        
        // Future integration point for Firebase Crashlytics / Analytics
        // Example: FirebaseCrashlytics.getInstance().log("$level: $tag: $message")
        // Example: if (throwable != null) FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}
