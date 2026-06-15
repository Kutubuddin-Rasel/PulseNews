package com.example.newsapp.data.util

import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.remote.dto.AiSummaryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import com.example.newsapp.BuildConfig
import com.example.newsapp.domain.util.AiSummarizer
import com.example.newsapp.domain.util.AiSummaryResult

@Singleton
class AiSummarizerImpl @Inject constructor(
    private val api: PulseBackendApi,
    @ApplicationContext private val context: Context
) : AiSummarizer {
    override suspend fun generateTlDr(articleId: String, articleText: String): AiSummaryResult = withContext(Dispatchers.IO) {
        val TAG = "AiSummarizer"
        try {
            val idempotencyKey = java.util.UUID.randomUUID().toString()
            Log.d(TAG, "Requesting AI Summary for articleId: $articleId with Idempotency-Key: $idempotencyKey")
            val response = api.getAiSummary(idempotencyKey, articleId, AiSummaryRequest(articleText))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    when (body.error) {
                        "RATE_LIMIT_EXCEEDED" -> {
                            Log.w(TAG, "RATE_LIMIT_EXCEEDED for article $articleId")
                            AiSummaryResult.RateLimitExceeded
                        }
                        "GENERATION_FAILED" -> {
                            // S1: only persist the raw article text in debug builds — in release this
                            // is a privacy leak + unbounded cacheDir growth.
                            if (BuildConfig.DEBUG) {
                                val debugFile = File(context.cacheDir, "failed_prompt_$articleId.txt")
                                try {
                                    debugFile.writeText(articleText)
                                    Log.e(TAG, "GENERATION_FAILED for article $articleId. Saved the exact text payload (${articleText.length} chars) to: ${debugFile.absolutePath} for debugging.")
                                } catch (e: Exception) {
                                    Log.e(TAG, "GENERATION_FAILED for article $articleId. Failed to save debug file.", e)
                                }
                            } else {
                                Log.e(TAG, "GENERATION_FAILED for article $articleId.")
                            }
                            AiSummaryResult.Error("GENERATION_FAILED")
                        }
                        null -> {
                            val summary = body.summary
                            if (summary.isNullOrBlank()) {
                                Log.e(TAG, "Success response but summary is null/blank for article $articleId")
                                AiSummaryResult.Error("Backend returned empty summary")
                            } else {
                                Log.d(TAG, "Successfully generated summary for article $articleId")
                                AiSummaryResult.Success(summary.trim())
                            }
                        }
                        else -> {
                            Log.e(TAG, "Unknown backend error for article $articleId: ${body.error}")
                            AiSummaryResult.Error("Backend error: ${body.error}")
                        }
                    }
                } else {
                    Log.e(TAG, "Response successful but body is null for article $articleId")
                    AiSummaryResult.Error("Empty response body")
                }
            } else {
                val errorBodyStr = response.errorBody()?.string()
                Log.e(TAG, "HTTP Error ${response.code()} for article $articleId. Body: $errorBodyStr")
                AiSummaryResult.Error("HTTP Error ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while generating summary for article $articleId", e)
            AiSummaryResult.Error(e.message ?: "Unknown network error")
        }
    }
}
