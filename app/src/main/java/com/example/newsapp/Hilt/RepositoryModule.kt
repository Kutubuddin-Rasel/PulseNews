package com.example.newsapp.Hilt

import com.example.newsapp.data.repository.NewsRepositoryImpl
import com.example.newsapp.data.repository.SavedArticleRepositoryImpl
import com.example.newsapp.data.util.AndroidConnectivityMonitor
import com.example.newsapp.data.util.SystemClockProvider
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.domain.repository.SavedArticleRepository
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
    abstract fun bindSavedArticleRepository(impl: SavedArticleRepositoryImpl): SavedArticleRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityMonitor(impl: AndroidConnectivityMonitor): ConnectivityMonitor

    @Binds
    @Singleton
    abstract fun bindClockProvider(impl: SystemClockProvider): ClockProvider
}
