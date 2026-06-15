package com.example.newsapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ArticleTaxonomyDto(
    @SerializedName("categories") val categories: List<String>? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("mlConfidence") val mlConfidence: Float? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("articleId") val articleId: String? = null
)

data class BookmarkRequest(
    @SerializedName("articleId") val articleId: String
)

data class AiSummaryRequest(
    @SerializedName("articleText") val articleText: String
)

data class AiSummaryResponse(
    @SerializedName("summary") val summary: String?,
    @SerializedName("error") val error: String?
)

data class PulseArticleDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("snippet") val snippet: String?,
    @SerializedName("pubDate") val pubDate: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("urlToImage") val urlToImage: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("provenance") val provenance: ProvenanceDto? = null,
    @SerializedName("regionCode") val regionCode: String? = null,
    @SerializedName("sourceTier") val sourceTier: Int? = null,
    // CONF1: backend emits `currentGravityScore`; bind the wire name explicitly so the
    // value stops deserializing to null (it previously read a non-existent `gravity_score`).
    @SerializedName("currentGravityScore") val gravity_score: Float? = null,
    @SerializedName("personalized_score") val personalized_score: Double? = null,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("taxonomy") val taxonomy: ArticleTaxonomyDto? = null
)
