package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedFeedDao {
    @Query("SELECT * FROM cached_feed_articles WHERE feedKey = :feedKey ORDER BY sortOrder ASC")
    fun getByFeedKey(feedKey: String): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("SELECT * FROM cached_feed_articles WHERE url = :url LIMIT 1")
    suspend fun findOneByUrl(url: String): CachedFeedArticleEntity?

    @Query("SELECT * FROM cached_feed_articles")
    suspend fun getAll(): List<CachedFeedArticleEntity>

    @Query("DELETE FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun clearFeed(feedKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedFeedArticleEntity>)

    @Query("SELECT MAX(fetchedAt) FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun latestFetchTime(feedKey: String): Long?

    @Query("""
        SELECT * FROM cached_feed_articles 
        WHERE (:source IS NULL OR sourceName = :source)
        AND feedKey = :categoryKey
        ORDER BY sortOrder ASC
    """)
    fun getFilteredFeedByCategory(categoryKey: String, source: String?): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    // F1: scope the source list to the active feed. Without the feedKey filter this leaked sources
    // from search results and every previously-visited category into the Home filter dropdown.
    @Query("SELECT DISTINCT sourceName FROM cached_feed_articles WHERE feedKey = :feedKey AND sourceName IS NOT NULL AND sourceName != '' ORDER BY sourceName ASC")
    fun getAvailableSources(feedKey: String): kotlinx.coroutines.flow.Flow<List<String>>

    @Query("SELECT url FROM cached_feed_articles WHERE feedKey = :feedKey ORDER BY sortOrder ASC LIMIT :limit")
    suspend fun getTopUrls(feedKey: String, limit: Int): List<String>
}
