package com.example.newsapp.domain.util

sealed class AiSummaryResult {
    data class Success(val summary: String) : AiSummaryResult()
    object RateLimitExceeded : AiSummaryResult()
    data class Error(val message: String) : AiSummaryResult()
}

interface AiSummarizer {
    suspend fun generateTlDr(articleId: String, articleText: String): AiSummaryResult
}
