package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedFeedDao {
    @Query("SELECT * FROM cached_feed_articles WHERE feedKey = :feedKey ORDER BY relevanceScore DESC, page ASC, sortOrder ASC")
    fun getByFeedKey(feedKey: String): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("SELECT * FROM cached_feed_articles WHERE url = :url LIMIT 1")
    suspend fun findOneByUrl(url: String): CachedFeedArticleEntity?

    @Query("SELECT * FROM cached_feed_articles")
    suspend fun getAll(): List<CachedFeedArticleEntity>

    @Query("UPDATE cached_feed_articles SET relevanceScore = :score WHERE id = :id")
    suspend fun updateRelevanceScore(id: Int, score: Float)

    @Query("DELETE FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun clearFeed(feedKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedFeedArticleEntity>)

    @Query("SELECT MAX(fetchedAt) FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun latestFetchTime(feedKey: String): Long?

    @androidx.room.RawQuery(observedEntities = [CachedFeedArticleEntity::class])
    fun getFilteredFeed(query: androidx.sqlite.db.SupportSQLiteQuery): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("SELECT DISTINCT sourceName FROM cached_feed_articles WHERE sourceName IS NOT NULL AND sourceName != '' ORDER BY sourceName ASC")
    fun getAvailableSources(): kotlinx.coroutines.flow.Flow<List<String>>

    @Query("SELECT url FROM cached_feed_articles WHERE feedKey = :feedKey ORDER BY page ASC, sortOrder ASC LIMIT :limit")
    suspend fun getTopUrls(feedKey: String, limit: Int): List<String>
}
