package com.example.newsapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleTaxonomyDto(
    @Json(name = "categories") val categories: List<String>? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "mlConfidence") val mlConfidence: Float? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "articleId") val articleId: String? = null
)

@JsonClass(generateAdapter = true)
data class BookmarkRequest(
    @Json(name = "articleId") val articleId: String
)

@JsonClass(generateAdapter = true)
data class AiSummaryRequest(
    @Json(name = "articleText") val articleText: String
)

@JsonClass(generateAdapter = true)
data class AiSummaryResponse(
    @Json(name = "summary") val summary: String?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class PulseArticleDto(
    @Json(name = "id") val id: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "link") val link: String?,
    @Json(name = "snippet") val snippet: String?,
    @Json(name = "pubDate") val pubDate: String?,
    @Json(name = "source") val source: String?,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "urlToImage") val urlToImage: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "provenance") val provenance: ProvenanceDto? = null,
    @Json(name = "regionCode") val regionCode: String? = null,
    @Json(name = "sourceTier") val sourceTier: Int? = null,
    // CONF1: backend emits `currentGravityScore`; bind the wire name explicitly so the
    // value stops deserializing to null (it previously read a non-existent `gravity_score`).
    @Json(name = "currentGravityScore") val gravity_score: Float? = null,
    @Json(name = "personalized_score") val personalized_score: Double? = null,
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "taxonomy") val taxonomy: ArticleTaxonomyDto? = null
)
