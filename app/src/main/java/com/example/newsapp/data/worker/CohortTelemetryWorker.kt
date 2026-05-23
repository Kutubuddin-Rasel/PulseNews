package com.example.newsapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.data.util.AppTelemetry
import com.example.newsapp.data.util.LocalEngagementTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class CohortTelemetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val localEngagementTracker: LocalEngagementTracker,
    private val appTelemetry: AppTelemetry
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Get the current cohort distribution
            val cohortDistribution = localEngagementTracker.getCohortDistribution().first()
            
            // Format it securely - NO PII, just aggregate percentages
            val payloadBuilder = StringBuilder().append("User Cohort Distribution: ")
            cohortDistribution.forEach { (categoryId, percentage) ->
                // Output percentage with 2 decimal places
                val pctString = String.format("%.2f%%", percentage * 100)
                payloadBuilder.append("[Cat $categoryId: $pctString] ")
            }
            
            // Send the anonymous payload to our telemetry interface
            // If the user has disabled telemetry, this will be safely dropped by AppTelemetry
            appTelemetry.info("CohortTelemetryWorker", payloadBuilder.toString())
            
            Result.success()
        } catch (e: Exception) {
            appTelemetry.error("CohortTelemetryWorker", "Failed to calculate and sync cohort", e)
            Result.retry()
        }
    }
}
