package com.example.newsapp.Hilt

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EngagementDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrivacyDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlgorithmDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TaxonomyDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FeedMetaDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GamificationDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReadingDataStore


@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @EngagementDataStore
    fun provideEngagementDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("local_engagement_prefs") }
        )

    @Provides
    @Singleton
    @PrivacyDataStore
    fun providePrivacyDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("privacy_prefs") }
        )

    @Provides
    @Singleton
    @AlgorithmDataStore
    fun provideAlgorithmDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("algorithm_prefs") }
        )

    @Provides
    @Singleton
    @TaxonomyDataStore
    fun provideTaxonomyDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("taxonomy_prefs") }
        )

    @Provides
    @Singleton
    @FeedMetaDataStore
    fun provideFeedMetaDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("feed_meta_prefs") }
        )

    // EG1: the gamification store was previously created by a file-scope `preferencesDataStore`
    // delegate inside EngagementTrackerImpl. The factory resolves to the same on-disk file
    // ("gamification_prefs.preferences_pb"), so existing streaks/counts are preserved.
    @Provides
    @Singleton
    @GamificationDataStore
    fun provideGamificationDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("gamification_prefs") }
        )

    @Provides
    @Singleton
    @ReadingDataStore
    fun provideReadingDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("reading_prefs") }
        )
}
