package com.example.newsapp.Hilt

import android.content.Context
import androidx.room.Room
import com.example.newsapp.Room.ArticleDatabase
import com.example.newsapp.Room.ArticleDao
import com.example.newsapp.Room.CachedFeedDao
import com.example.newsapp.Room.DatabaseMigrations
import com.example.newsapp.Room.InteractionEventDao
import com.example.newsapp.Room.TrendingTopicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): androidx.work.WorkManager {
        return androidx.work.WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideRoom(@ApplicationContext context: Context): ArticleDatabase {
        return Room.databaseBuilder(context, ArticleDatabase::class.java, "ArticleDB")
            // O4: upgrades now run real migrations (DatabaseMigrations) instead of dropping the DB,
            // so a schema bump preserves the user's saved articles / paging state. Every future
            // version MUST add its Migration to DatabaseMigrations.ALL or the upgrade fails loudly.
            .addMigrations(*DatabaseMigrations.ALL)
            // Destructive fallback is retained ONLY for downgrades (a dev installing an older build
            // over a newer DB) — there is no forward migration for that direction, and it only ever
            // touches re-syncable dev data (saved articles re-hydrate via BookmarkSyncWorker).
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideArticleDao(database: ArticleDatabase): ArticleDao {
        return database.articledao()
    }

    @Provides
    fun provideCachedFeedDao(database: ArticleDatabase): CachedFeedDao {
        return database.cachedFeedDao()
    }

    @Provides
    fun provideInteractionEventDao(database: ArticleDatabase): InteractionEventDao {
        return database.interactionEventDao()
    }

    @Provides
    fun provideTrendingTopicDao(database: ArticleDatabase): TrendingTopicDao {
        return database.trendingTopicDao()
    }

    @Provides
    fun provideFeedRemoteKeyDao(database: ArticleDatabase): com.example.newsapp.Room.FeedRemoteKeyDao {
        return database.feedRemoteKeyDao()
    }
}
