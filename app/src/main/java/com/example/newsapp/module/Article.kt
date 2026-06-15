package com.example.newsapp.module

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class ArticleTaxonomy(
    val categories: List<String>,
    val tags: List<String>,
    val mlConfidence: Float?,
    val id: String?,
    val articleId: String?
)

@Entity(
    tableName = "saved_articles",
    indices = [Index(value = ["url"], unique = true)]
)
data class Article(
    @PrimaryKey
    val url: String,
    val backendId: String = "",
    val author: String?,
    val content: String?,
    val description: String?,
    val publishedAt: String?,
    val source: Source,
    val title: String,
    val urlToImage: String?,
    // CONF2: feed-supplied summary; the reader shows this when present and only falls
    // back to the on-demand /summary endpoint when it is null.
    val summary: String? = null,
    val provenance: com.example.newsapp.domain.model.Provenance? = null,
    val regionCode: String? = null,
    val sourceTier: Int? = null,
    val gravity_score: Float? = null,
    val personalized_score: Double? = null,
    val distance: Double? = null,
    @androidx.room.Embedded(prefix = "taxonomy_")
    val taxonomy: ArticleTaxonomy? = null
)
