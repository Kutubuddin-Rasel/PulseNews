package com.example.newsapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TelemetryEventDto(
    val articleId: String,
    val interactionType: String,
    val timestamp: Long
)

@Serializable
data class TelemetryBatchRequest(
    val events: List<TelemetryEventDto>
)
