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
    val provenance: com.example.newsapp.domain.model.Provenance? = null,
    val regionCode: String? = null,
    val sourceTier: Int? = null,
    val gravity_score: Float? = null,
    @androidx.room.Embedded(prefix = "taxonomy_")
    val taxonomy: ArticleTaxonomy? = null
)
