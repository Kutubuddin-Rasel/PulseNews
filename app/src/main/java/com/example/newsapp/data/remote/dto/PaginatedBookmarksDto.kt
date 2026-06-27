package com.example.newsapp.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaginatedBookmarksDto(
    val items: List<PulseArticleDto>,
    val nextCursor: String?
)
