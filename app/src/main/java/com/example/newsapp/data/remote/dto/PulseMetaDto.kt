package com.example.newsapp.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PulseMetaDto(
    val totalPages: Int,
    val lastUpdated: String
)
