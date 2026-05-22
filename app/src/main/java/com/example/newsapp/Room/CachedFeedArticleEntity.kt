package com.example.newsapp.Room

import androidx.room.Entity

@Entity(
    tableName = "cached_feed_articles",
    primaryKeys = ["feedKey", "page", "url"]
)
data class CachedFeedArticleEntity(
    val feedKey: String,
    val page: Int,
    val url: String,
    val author: String?,
    val content: String?,
    val description: String?,
    val publishedAt: String?,
    val sourceId: String?,
    val sourceName: String,
    val title: String,
    val urlToImage: String?,
    val sortOrder: Int,
    val fetchedAt: Long
)
