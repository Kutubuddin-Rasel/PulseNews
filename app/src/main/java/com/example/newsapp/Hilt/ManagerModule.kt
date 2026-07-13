package com.example.newsapp.Hilt

import com.example.newsapp.data.util.*
import com.example.newsapp.domain.manager.*
import com.example.newsapp.domain.tracker.*
import com.example.newsapp.domain.util.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagerModule {

    @Binds
    @Singleton
    abstract fun bindFirestoreSyncManager(impl: FirestoreSyncManagerImpl): FirestoreSyncManager

    @Binds
    @Singleton
    abstract fun bindEngagementTracker(impl: EngagementTrackerImpl): EngagementTracker

    @Binds
    @Singleton
    abstract fun bindLocalEngagementTracker(impl: LocalEngagementTrackerImpl): LocalEngagementTracker

    @Binds
    @Singleton
    abstract fun bindAppTelemetry(impl: AppTelemetryImpl): AppTelemetry

    @Binds
    @Singleton
    abstract fun bindAiSummarizer(impl: AiSummarizerImpl): AiSummarizer

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: DeviceIdProviderImpl): DeviceIdProvider
}
