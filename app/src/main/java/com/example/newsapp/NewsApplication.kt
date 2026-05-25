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

        // Queue the taxonomy sync worker (Hybrid ML Dictionary)
        val taxonomyConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val taxonomySyncRequest = PeriodicWorkRequestBuilder<com.example.newsapp.worker.TaxonomySyncWorker>(
            3, TimeUnit.DAYS
        ).setConstraints(taxonomyConstraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaxonomySyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
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
    }
}