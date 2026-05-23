package com.example.newsapp.data.util

import com.example.newsapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSummarizer @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content {
                text("You are an expert news editor. Provide exactly 3 concise bullet points summarizing the following article. Do not include any introductory or concluding text. Your output must strictly be three bullet points, each starting with a bullet character.")
            }
        )
    }

    suspend fun generateTlDr(articleText: String): Result<String> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in local.properties"))
        }

        try {
            // We pass the article text. Gemini handles the rest based on the system instruction.
            val response = generativeModel.generateContent(articleText)
            val summary = response.text
            if (summary.isNullOrBlank()) {
                Result.failure(Exception("AI returned empty response"))
            } else {
                Result.success(summary.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
