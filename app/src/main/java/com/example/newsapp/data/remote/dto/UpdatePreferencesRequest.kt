package com.example.newsapp.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * Body for PUT api/user/preferences. Both fields are nullable so a caller can
 * persist just the region (Wave 1) or just the languages (Wave 2) without
 * clobbering the other on the server.
 */
@JsonClass(generateAdapter = true)
data class UpdatePreferencesRequest(
    val preferredRegions: List<String>? = null,
    val preferredLanguages: List<String>? = null
)
