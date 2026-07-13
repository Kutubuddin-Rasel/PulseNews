package com.example.newsapp.data.remote

import com.example.newsapp.data.remote.dto.TelemetryEvent
import com.example.newsapp.domain.model.VerificationStatus
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Serializes the sealed [VerificationStatus] as its `name` string (and back), so it round-trips
 * through Moshi without reflection. Used by the Room TypeConverter for the persisted `Provenance`.
 * Unknown values degrade to [VerificationStatus.UNVERIFIED] rather than throwing.
 */
class VerificationStatusAdapter {
    @ToJson
    fun toJson(status: VerificationStatus): String = status.name

    @FromJson
    fun fromJson(value: String): VerificationStatus =
        runCatching { VerificationStatus.valueOf(value) }.getOrDefault(VerificationStatus.UNVERIFIED)
}

/**
 * Hand-written adapter for [TelemetryEvent]. Its `data` is already-serialized JSON (one row per
 * event in Room) that must reach the wire verbatim: routing it through `Map<String, Any?>` would
 * let Moshi's generic `Any` adapter coerce every number to a Double (`42` → `42.0`), breaking the
 * Rust telemetry worker's integer parse. [JsonWriter.valueSink] copies the stored bytes through
 * untouched.
 *
 * Written by hand (rather than `@JsonClass` + a `@JsonQualifier`) because Moshi's KSP2 codegen
 * can't currently process a custom qualifier annotation on a property.
 */
class TelemetryEventAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, event: TelemetryEvent) {
        writer.beginObject()
        writer.name("type").value(event.type)
        writer.name("articleId").value(event.articleId)
        writer.name("timestamp").value(event.timestamp)
        writer.name("data")
        writer.valueSink().use { sink -> sink.writeUtf8(event.data) }
        writer.endObject()
    }

    @FromJson
    fun fromJson(reader: JsonReader): TelemetryEvent {
        var type = ""
        var articleId = ""
        var timestamp = ""
        var data = "{}"
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> type = reader.nextString()
                "articleId" -> articleId = reader.nextString()
                "timestamp" -> timestamp = reader.nextString()
                "data" -> data = reader.nextSource().use { it.readUtf8() }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return TelemetryEvent(type, articleId, timestamp, data)
    }
}
