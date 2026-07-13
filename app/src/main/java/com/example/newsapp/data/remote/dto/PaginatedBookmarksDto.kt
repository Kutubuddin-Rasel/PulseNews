package com.example.newsapp.data.remote.dto

data class PaginatedBookmarksDto(
    val items: List<PulseArticleDto>,
    val nextCursor: String?
)
