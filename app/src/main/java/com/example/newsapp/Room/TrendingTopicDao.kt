package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrendingTopicDao {
    @Query("SELECT * FROM trending_topics ORDER BY count DESC")
    fun getTrendingTopics(): Flow<List<TrendingTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TrendingTopicEntity>)

    @Query("DELETE FROM trending_topics")
    suspend fun clearTopics()
}
