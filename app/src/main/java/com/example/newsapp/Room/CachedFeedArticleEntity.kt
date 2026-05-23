package com.example.newsapp.Room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_feed_articles",
    indices = [
        Index(value = ["feedKey", "page", "url"], unique = true),
        Index(value = ["sourceName"])
    ]
)
data class CachedFeedArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
    val fetchedAt: Long,
    val relevanceScore: Float = 0f,
    @androidx.room.ColumnInfo(name = "verification_status") val verificationStatus: String = "UNVERIFIED",
    @androidx.room.ColumnInfo(name = "signature_protocol") val signatureProtocol: String? = null,
    @androidx.room.ColumnInfo(name = "trusted_signer") val trustedSigner: String? = null
)
