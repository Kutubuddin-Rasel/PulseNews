package com.example.newsapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.newsapp.Hilt.ApplicationScope
import com.example.newsapp.data.worker.NewsSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NewsApplication: Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    // IMG1: Coil calls newImageLoader() lazily on the first image request — by then Hilt has
    // field-injected this app in super.onCreate(), so the tuned singleton is ready.
    @Inject
    lateinit var imageLoader: ImageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        // O2: enqueueing five periodic/one-time works touches the WorkManager DB on disk. Doing it
        // inline on the main thread blocks first-frame startup; run it on the app-scoped IO scope.
        appScope.launch { setupBackgroundSync() }
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

        // Queue interaction telemetry worker.
        // CONNECTED (not UNMETERED): telemetry payloads are tiny, and cellular-only users must
        // still drain their interaction_events queue instead of growing it unbounded (audit A7).
        val interactionConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val interactionSyncRequest = PeriodicWorkRequestBuilder<com.example.newsapp.data.worker.TelemetrySyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(interactionConstraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "InteractionTelemetryWork",
            ExistingPeriodicWorkPolicy.KEEP,
            interactionSyncRequest
        )

        // Queue trending topics sync worker
        val trendingSyncRequest = PeriodicWorkRequestBuilder<com.example.newsapp.data.worker.TrendingSyncWorker>(
            3, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrendingSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            trendingSyncRequest
        )
    }
}