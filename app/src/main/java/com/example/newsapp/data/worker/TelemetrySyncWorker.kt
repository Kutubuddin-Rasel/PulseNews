package com.example.newsapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.Room.InteractionEventDao
import com.example.newsapp.data.remote.dto.TelemetryBatchRequest
import com.example.newsapp.data.remote.dto.TelemetryEventDto
import com.example.newsapp.data.util.DeviceIdProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TelemetrySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val interactionEventDao: InteractionEventDao,
    private val pulseBackendApi: PulseBackendApi,
    private val deviceIdProvider: DeviceIdProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val deviceId = deviceIdProvider.deviceId
        
        // Grab up to 50 pending events
        val pendingEvents = interactionEventDao.getPendingEvents(50)
        if (pendingEvents.isEmpty()) {
            return Result.success()
        }

        val dtoList = pendingEvents.map { 
            TelemetryEventDto(
                articleId = it.articleId,
                interactionType = it.interactionType,
                timestamp = it.timestamp
            )
        }
        
        val request = TelemetryBatchRequest(events = dtoList)

        return try {
            val response = pulseBackendApi.postInteractions(deviceId, request)
            if (response.isSuccessful) {
                // If success, delete those events from DB
                val eventIds = pendingEvents.map { it.id }
                interactionEventDao.deleteEvents(eventIds)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
