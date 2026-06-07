package com.example.newsapp.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TelemetryBatchRequest(
    val events: List<TelemetryEvent>
)

@Serializable
data class TelemetryEvent(
    val type: String,
    val articleId: String,
    val timestamp: String,
    val data: JsonObject
)
