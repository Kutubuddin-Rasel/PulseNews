package com.example.newsapp.Room

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = CachedFeedArticleEntity::class)
@Entity(tableName = "cached_feed_fts")
data class CachedFeedFtsEntity(
    val title: String,
    val description: String?
)
