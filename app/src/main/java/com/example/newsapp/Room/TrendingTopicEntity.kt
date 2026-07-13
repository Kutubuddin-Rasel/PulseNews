package com.example.newsapp.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trending_topics")
data class TrendingTopicEntity(
    @PrimaryKey val tag: String,
    val count: Int,
    val lastUpdated: Long
)
