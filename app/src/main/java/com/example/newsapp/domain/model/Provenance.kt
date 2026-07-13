package com.example.newsapp.domain.model

sealed interface VerificationStatus {
    val name: String

    data object SOURCE_VERIFIED : VerificationStatus { override val name = "SOURCE_VERIFIED" }
    data object UNVERIFIED : VerificationStatus { override val name = "UNVERIFIED" }

    companion object {
        fun valueOf(value: String): VerificationStatus = when (value) {
            "SOURCE_VERIFIED" -> SOURCE_VERIFIED
            "UNVERIFIED" -> UNVERIFIED
            else -> throw IllegalArgumentException("No object com.example.newsapp.domain.model.VerificationStatus.$value")
        }
    }
}

data class Provenance(
    val status: VerificationStatus,
    val verificationMethod: String?,
    val trustedSigner: String?
)
