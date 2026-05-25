package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedFeedDao {
    @Query("SELECT * FROM cached_feed_articles WHERE feedKey = :feedKey ORDER BY relevanceScore DESC, fetchedAt ASC, sortOrder ASC")
    fun getByFeedKey(feedKey: String): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("SELECT * FROM cached_feed_articles WHERE url = :url LIMIT 1")
    suspend fun findOneByUrl(url: String): CachedFeedArticleEntity?

    @Query("SELECT * FROM cached_feed_articles")
    suspend fun getAll(): List<CachedFeedArticleEntity>

    // Added for O(1) memory and time complexity keyset pagination chunking
    @Query("SELECT * FROM cached_feed_articles WHERE id > :lastId ORDER BY id ASC LIMIT :limit")
    suspend fun getChunkByKeyset(lastId: Int, limit: Int = 500): List<CachedFeedArticleEntity>

    @Query("UPDATE cached_feed_articles SET relevanceScore = :score WHERE id = :id")
    suspend fun updateRelevanceScore(id: Int, score: Float)

    // Bulk update for high-performance chunked processing
    @androidx.room.Update
    suspend fun updateAll(entities: List<CachedFeedArticleEntity>)

    @Query("DELETE FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun clearFeed(feedKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedFeedArticleEntity>)

    @Query("SELECT MAX(fetchedAt) FROM cached_feed_articles WHERE feedKey = :feedKey")
    suspend fun latestFetchTime(feedKey: String): Long?

    @Query("""
        SELECT cached_feed_articles.* FROM cached_feed_articles 
        JOIN cached_feed_fts ON cached_feed_articles.id = cached_feed_fts.rowid 
        WHERE cached_feed_fts MATCH :matchQuery
        AND cached_feed_articles.sourceName = :source
        ORDER BY 
          cached_feed_articles.relevanceScore DESC,
          cached_feed_articles.fetchedAt ASC, 
          cached_feed_articles.sortOrder ASC
    """)
    fun getFilteredFeedWithMatchAndSource(matchQuery: String, source: String): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("""
        SELECT cached_feed_articles.* FROM cached_feed_articles 
        JOIN cached_feed_fts ON cached_feed_articles.id = cached_feed_fts.rowid 
        WHERE cached_feed_fts MATCH :matchQuery
        ORDER BY 
          cached_feed_articles.relevanceScore DESC,
          cached_feed_articles.fetchedAt ASC, 
          cached_feed_articles.sortOrder ASC
    """)
    fun getFilteredFeedWithMatch(matchQuery: String): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("""
        SELECT * FROM cached_feed_articles 
        WHERE sourceName = :source
        ORDER BY 
          relevanceScore DESC,
          fetchedAt ASC, 
          sortOrder ASC
    """)
    fun getFilteredFeedWithSource(source: String): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("""
        SELECT * FROM cached_feed_articles 
        ORDER BY 
          relevanceScore DESC,
          fetchedAt ASC, 
          sortOrder ASC
    """)
    fun getFilteredFeedAll(): androidx.paging.PagingSource<Int, CachedFeedArticleEntity>

    @Query("SELECT DISTINCT sourceName FROM cached_feed_articles WHERE sourceName IS NOT NULL AND sourceName != '' ORDER BY sourceName ASC")
    fun getAvailableSources(): kotlinx.coroutines.flow.Flow<List<String>>

    @Query("SELECT url FROM cached_feed_articles WHERE feedKey = :feedKey ORDER BY fetchedAt ASC, sortOrder ASC LIMIT :limit")
    suspend fun getTopUrls(feedKey: String, limit: Int): List<String>
}
