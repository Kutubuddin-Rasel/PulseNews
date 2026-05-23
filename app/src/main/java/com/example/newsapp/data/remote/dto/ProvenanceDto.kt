package com.example.newsapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProvenanceDto(
    @SerialName("status")
    val status: String? = null,
    @SerialName("verification_method")
    val verificationMethod: String? = null,
    @SerialName("trusted_signer")
    val trustedSigner: String? = null
)
