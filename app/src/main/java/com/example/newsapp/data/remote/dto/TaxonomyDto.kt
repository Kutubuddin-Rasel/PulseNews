package com.example.newsapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TaxonomyDto(
    @Json(name = "version")
    val version: String,

    @Json(name = "categories")
    val categories: Map<String, List<String>>
)
