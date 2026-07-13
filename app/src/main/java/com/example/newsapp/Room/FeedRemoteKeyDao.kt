package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FeedRemoteKeyDao {
    @Query("SELECT * FROM feed_remote_keys WHERE feedKey = :feedKey LIMIT 1")
    suspend fun remoteKey(feedKey: String): FeedRemoteKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(key: FeedRemoteKey)

    @Query("DELETE FROM feed_remote_keys WHERE feedKey = :feedKey")
    suspend fun clear(feedKey: String)
}
