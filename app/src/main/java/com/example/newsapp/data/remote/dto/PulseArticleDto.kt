package com.example.newsapp.data.remote.dto

data class ArticleTaxonomyDto(
    val categories: List<String>? = null,
    val tags: List<String>? = null
)

data class BookmarkRequest(
    val articleId: String
)

data class PulseArticleDto(
    val id: String,
    val title: String,
    val link: String,
    val snippet: String,
    val pubDate: String,
    val source: String,
    val urlToImage: String? = null,
    val provenance: ProvenanceDto? = null,
    val regionCode: String? = null,
    val sourceTier: Int? = null,
    val taxonomy: ArticleTaxonomyDto? = null
)
