package com.example.newsapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProvenanceDto(
    @Json(name = "status")
    val status: String? = null,
    @Json(name = "verification_method")
    val verificationMethod: String? = null,
    @Json(name = "trusted_signer")
    val trustedSigner: String? = null
)
