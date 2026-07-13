package com.example.newsapp.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TrendingTopicDto(
    val tag: String,
    val count: Int
)
