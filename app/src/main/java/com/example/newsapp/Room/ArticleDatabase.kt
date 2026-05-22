package com.example.newsapp.Room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.newsapp.module.Article

@Database(
    entities = [Article::class, CachedFeedArticleEntity::class, RemoteKeys::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(TypeConverter::class)
abstract class ArticleDatabase : RoomDatabase() {
    abstract fun articledao(): ArticleDao
    abstract fun cachedFeedDao(): CachedFeedDao
    abstract fun remoteKeysDao(): RemoteKeysDao
}
