package com.example.newsapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.newsapp.data.worker.NewsSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NewsApplication: Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupBackgroundSync()
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NewsSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // Queue the taxonomy sync worker (App Launch Check)
        val taxonomyConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val taxonomySyncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.newsapp.worker.TaxonomySyncWorker>()
            .setConstraints(taxonomyConstraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "TaxonomySyncWork_Launch",
            androidx.work.ExistingWorkPolicy.KEEP,
            taxonomySyncRequest
        )

        // Queue the weekly telemetry worker
        val cohortTelemetryRequest = PeriodicWorkRequestBuilder<com.example.newsapp.data.worker.CohortTelemetryWorker>(
            7, java.util.concurrent.TimeUnit.DAYS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CohortTelemetryWork",
            ExistingPeriodicWorkPolicy.KEEP,
            cohortTelemetryRequest
        )

        // Queue interaction telemetry worker
        val interactionConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val interactionSyncRequest = PeriodicWorkRequestBuilder<com.example.newsapp.data.worker.TelemetrySyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(interactionConstraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "InteractionTelemetryWork",
            ExistingPeriodicWorkPolicy.KEEP,
            interactionSyncRequest
        )
    }
}