package com.example.newsapp.data.remote.dto

import androidx.annotation.Keep

@Keep
data class TelemetryEventDto(
    val articleId: String,
    val interactionType: String,
    val timestamp: Long
)

@Keep
data class TelemetryBatchRequest(
    val events: List<TelemetryEventDto>
)
