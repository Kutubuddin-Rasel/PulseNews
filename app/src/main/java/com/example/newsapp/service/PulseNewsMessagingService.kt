package com.example.newsapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.newsapp.MainActivity
import com.example.newsapp.data.repository.NotificationPreferencesRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class PulseNewsMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationPreferencesRepository: NotificationPreferencesRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            serviceScope.launch {
                handleNotification(remoteMessage.data)
            }
        }
    }

    private suspend fun handleNotification(data: Map<String, String>) {
        val title = data["title"] ?: "New Article"
        val message = data["message"] ?: "Tap to read more."
        val url = data["url"]

        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val quietHoursEnabled = notificationPreferencesRepository.quietHoursEnabled.first()
        val startMinutes = notificationPreferencesRepository.quietHoursStartMinutes.first()
        val endMinutes = notificationPreferencesRepository.quietHoursEndMinutes.first()

        // Check Quiet Hours
        if (quietHoursEnabled) {
            val isQuietTime = if (startMinutes < endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else { // Wraps around midnight
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
            if (isQuietTime) return // Drop notification
        }

        // Check daily limits
        val currentDayMillis = getStartOfDayMillis(calendar)
        val lastResetDate = notificationPreferencesRepository.lastResetDate.first()
        
        if (lastResetDate < currentDayMillis) {
            notificationPreferencesRepository.resetDailyCountIfNeeded(currentDayMillis)
        }

        val currentCount = notificationPreferencesRepository.currentDailyCount.first()
        val maxLimit = notificationPreferencesRepository.maxDailyNotifications.first()

        if (currentCount >= maxLimit) {
            return // Drop notification
        }

        // We can show it!
        showNotification(title, message, url)
        notificationPreferencesRepository.incrementDailyCount()
    }

    private fun showNotification(title: String, message: String, url: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pulse_news_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "News Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Breaking news and topics"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            url?.let { putExtra("url", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon later
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server if needed
    }

    private fun getStartOfDayMillis(calendar: Calendar): Long {
        val clone = calendar.clone() as Calendar
        clone.set(Calendar.HOUR_OF_DAY, 0)
        clone.set(Calendar.MINUTE, 0)
        clone.set(Calendar.SECOND, 0)
        clone.set(Calendar.MILLISECOND, 0)
        return clone.timeInMillis
    }
}
