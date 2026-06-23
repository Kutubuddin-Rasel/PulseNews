package com.example.newsapp.Hilt

import com.example.newsapp.data.repository.NewsRepositoryImpl
import com.example.newsapp.data.repository.SavedArticleRepositoryImpl
import com.example.newsapp.data.util.AndroidConnectivityMonitor
import com.example.newsapp.data.util.SystemClockProvider
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.domain.repository.FeedMetaRepository
import com.example.newsapp.domain.util.ClockProvider
import com.example.newsapp.domain.util.ConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindFeedMetaRepository(impl: com.example.newsapp.data.repository.FeedMetaRepositoryImpl): FeedMetaRepository

    @Binds
    @Singleton
    abstract fun bindSavedArticleRepository(impl: SavedArticleRepositoryImpl): SavedArticleRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityMonitor(impl: AndroidConnectivityMonitor): ConnectivityMonitor

    @Binds
    @Singleton
    abstract fun bindClockProvider(impl: SystemClockProvider): ClockProvider

    @Binds
    @Singleton
    abstract fun bindAlgorithmPreferencesRepository(impl: com.example.newsapp.data.repository.AlgorithmPreferencesRepositoryImpl): com.example.newsapp.domain.repository.AlgorithmPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindReadingPreferencesRepository(impl: com.example.newsapp.data.repository.ReadingPreferencesRepositoryImpl): com.example.newsapp.domain.repository.ReadingPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindNotificationPreferencesRepository(impl: com.example.newsapp.data.repository.NotificationPreferencesRepositoryImpl): com.example.newsapp.domain.repository.NotificationPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindPrivacyPreferencesRepository(impl: com.example.newsapp.data.repository.PrivacyPreferencesRepositoryImpl): com.example.newsapp.domain.repository.PrivacyPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindTaxonomyRepository(impl: com.example.newsapp.data.repository.TaxonomyRepositoryImpl): com.example.newsapp.domain.repository.TaxonomyRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: com.example.newsapp.data.util.AuthRepositoryImpl): com.example.newsapp.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun bindRecentSearchRepository(impl: com.example.newsapp.data.repository.RecentSearchRepositoryImpl): com.example.newsapp.domain.repository.RecentSearchRepository

    @Binds
    @Singleton
    abstract fun bindGeoLanguageRepository(impl: com.example.newsapp.data.repository.GeoLanguageRepositoryImpl): com.example.newsapp.domain.repository.GeoLanguageRepository
}
