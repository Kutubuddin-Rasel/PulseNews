package com.example.newsapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.Room.InteractionEventDao
import com.example.newsapp.Room.InteractionEventEntity
import com.example.newsapp.data.remote.dto.TelemetryBatchRequest
import com.example.newsapp.data.remote.dto.TelemetryEvent
import com.example.newsapp.domain.util.DeviceIdProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.time.Instant
import java.util.UUID

@HiltWorker
class TelemetrySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val interactionEventDao: InteractionEventDao,
    private val pulseBackendApi: PulseBackendApi,
    private val deviceIdProvider: DeviceIdProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        var drained = 0

        // Drain in batches until the queue is empty or we hit the per-run cap (audit A7).
        while (drained < MAX_EVENTS_PER_RUN) {
            val batch = interactionEventDao.getPendingEvents(BATCH_SIZE)
            if (batch.isEmpty()) break

            // Deterministic per-batch key: unique per distinct batch, identical across retries of
            // the same surviving rows — honours POST_news_telemetry.md idempotency contract (A6).
            val idempotencyKey = batchIdempotencyKey(batch)
            val request = TelemetryBatchRequest(events = batch.map { it.toTelemetryEvent() })

            val response = try {
                pulseBackendApi.postInteractions(idempotencyKey, request)
            } catch (e: IOException) {
                return Result.retry() // transient network → back off and retry the run
            } catch (e: Exception) {
                return Result.retry()
            }

            when {
                response.isSuccessful -> {
                    interactionEventDao.deleteEvents(batch.map { it.id })
                    drained += batch.size
                }
                // 401/403: not signed in / token invalid — retrying now cannot help. Keep the
                // events for a future authed run and stop; the table is bounded below (CONF4/A7).
                response.code() == 401 || response.code() == 403 -> break
                // 429 rate-limited or 5xx server error: back off and retry the whole run.
                response.code() == 429 || response.code() >= 500 -> return Result.retry()
                // Other 4xx (e.g. 400 malformed): poison batch — drop so it can't block the queue.
                else -> {
                    interactionEventDao.deleteEvents(batch.map { it.id })
                    drained += batch.size
                }
            }
        }

        // Always bound local storage, regardless of drain outcome (audit A7).
        interactionEventDao.trimToMostRecent(MAX_RETAINED_EVENTS)
        return Result.success()
    }

    private fun batchIdempotencyKey(batch: List<InteractionEventEntity>): String =
        UUID.nameUUIDFromBytes(
            batch.map { it.id }.sorted().joinToString(",").toByteArray()
        ).toString()

    private fun InteractionEventEntity.toTelemetryEvent(): TelemetryEvent {
        // `eventData` is already a serialized JSON object (written by AppTelemetryImpl); forward it
        // verbatim via the @RawJson field so numeric values keep their integer form on the wire.
        return TelemetryEvent(
            type = interactionType,
            articleId = articleId,
            timestamp = Instant.ofEpochMilli(timestamp).toString(),
            data = eventData?.takeIf { it.isNotBlank() } ?: "{}"
        )
    }

    companion object {
        private const val BATCH_SIZE = 50
        /** Cap per run so a huge backlog can't hold a wakelock indefinitely; the rest drains next run. */
        private const val MAX_EVENTS_PER_RUN = 500
        /** Hard ceiling on locally queued events to keep the table bounded when offline/unauthed. */
        private const val MAX_RETAINED_EVENTS = 5_000
    }
}
