package com.example.newsapp.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ProvenanceDto(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("verification_method")
    val verificationMethod: String? = null,
    @SerializedName("trusted_signer")
    val trustedSigner: String? = null
)
