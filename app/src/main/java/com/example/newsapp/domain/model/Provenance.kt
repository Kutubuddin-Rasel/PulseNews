package com.example.newsapp.domain.model

enum class VerificationStatus {
    SOURCE_VERIFIED,
    UNVERIFIED
}

data class Provenance(
    val status: VerificationStatus,
    val verificationMethod: String?,
    val trustedSigner: String?
)
