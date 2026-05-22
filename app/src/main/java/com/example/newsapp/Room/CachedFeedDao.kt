package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedFeedDao {
    @Query("SELECT * FROM cached_feed_articles WHERE feedKey = :feedKey ORDER BY page ASC, sortOrder ASC")
    fun getByFeedKey(feedKey: String): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("SELECT * FROM cached_feed_articles WHERE url = :url LIMIT 1")
    suspend fun findOneByUrl(url: String): CachedFeedArticleEntity?

    @Query("DELETE FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun clearFeed(feedKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedFeedArticleEntity>)

    @Query("SELECT MAX(fetchedAt) FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun latestFetchTime(feedKey: String): Long?
}
