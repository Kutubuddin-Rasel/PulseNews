package com.example.newsapp.data.remote.dto

import com.google.gson.JsonObject

data class TelemetryBatchRequest(
    val events: List<TelemetryEvent>
)

data class TelemetryEvent(
    val type: String,
    val articleId: String,
    val timestamp: String,
    val data: JsonObject
)
