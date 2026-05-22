package com.example.newsapp.Hilt

import android.content.Context
import androidx.room.Room
import com.example.newsapp.Room.ArticleDatabase
import com.example.newsapp.Room.ArticleDao
import com.example.newsapp.Room.CachedFeedDao
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
    fun provideRoom(@ApplicationContext context: Context): ArticleDatabase {
        return Room.databaseBuilder(context, ArticleDatabase::class.java, "ArticleDB")
            .fallbackToDestructiveMigration(dropAllTables = true)
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
}
