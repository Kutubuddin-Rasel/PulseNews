package com.example.newsapp.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TelemetryBatchRequest(
    val events: List<TelemetryEvent>
)

// Serialized by the hand-written TelemetryEventAdapter (registered on the Moshi instance), which
// writes `data` — an already-serialized JSON object — to the wire verbatim. Kept off @JsonClass
// codegen so the raw-JSON passthrough doesn't need a @JsonQualifier (unsupported by Moshi's KSP2).
data class TelemetryEvent(
    val type: String,
    val articleId: String,
    val timestamp: String,
    val data: String
)
