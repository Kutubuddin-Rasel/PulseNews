package com.example.newsapp.domain.util

import com.example.newsapp.module.Article
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object FeedScorer {

    private val TOPIC_KEYWORDS = mapOf(
        "tech" to listOf("tech", "technology", "software", "ai", "apple", "google", "microsoft", "cyber"),
        "politics" to listOf("politics", "government", "election", "president", "congress", "senate", "policy"),
        "business" to listOf("economy", "stock", "market", "finance", "business", "inflation", "corporate"),
        "sports" to listOf("sports", "football", "basketball", "soccer", "nfl", "nba", "championship", "athlete"),
        "entertainment" to listOf("movie", "music", "hollywood", "celebrity", "entertainment", "actor", "award")
    )

    private val TRUSTED_SOURCES = listOf("reuters", "ap", "associated press", "bbc", "npr", "bloomberg")

    /**
     * Computes an enterprise-grade relevance score for an article.
     * Takes into account:
     * 1. Topic Affinity (User-Steered DataStore Weights)
     * 2. Keyword Frequency (TF-IDF proxy: Title vs Content)
     * 3. Recency Decay (News ages quickly)
     * 4. Source Trustworthiness (Bonus for highly credible wire services)
     */
    fun computeScore(
        article: Article,
        userWeights: Map<String, Float>,
        currentTimeMillis: Long
    ): Float {
        var score = 0f

        val titleLower = article.title.lowercase(Locale.getDefault())
        val contentLower = article.content?.lowercase(Locale.getDefault()) ?: ""
        val descriptionLower = article.description?.lowercase(Locale.getDefault()) ?: ""

        val fullText = "$contentLower $descriptionLower"

        // 1 & 2. Topic Affinity & Keyword Frequency
        TOPIC_KEYWORDS.forEach { (topic, keywords) ->
            // Default weight is 0.5f if the user hasn't explicitly set it yet
            val userWeight = userWeights[topic] ?: 0.5f 

            var topicMatches = 0f
            keywords.forEach { keyword ->
                // Title matches are extremely strong indicators (weight 3x)
                if (titleLower.contains(keyword)) {
                    topicMatches += 3.0f
                }
                
                // Content matches (weight 1x) - fast simple frequency
                topicMatches += fullText.split(keyword).size - 1
            }

            // Apply the user's explicit slider weight
            score += (topicMatches * userWeight)
        }

        // 3. Source Trustworthiness
        val sourceLower = article.source.name.lowercase(Locale.getDefault())
        if (TRUSTED_SOURCES.any { sourceLower.contains(it) }) {
            score += 2.0f // Flat bonus for highly trusted wire services
        }

        // 4. Recency Decay
        // News loses relevance. We apply a decay multiplier based on age.
        var decayMultiplier = 1.0f
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val publishedDate = article.publishedAt?.let { dateFormat.parse(it) }
            
            if (publishedDate != null) {
                val ageInMillis = currentTimeMillis - publishedDate.time
                val ageInHours = ageInMillis / (1000 * 60 * 60).toFloat()
                
                if (ageInHours > 0) {
                    // Decay by 5% every hour, down to a floor of 20% value.
                    // A 24-hour old article is worth much less than a 1-hour old article.
                    decayMultiplier = maxOf(0.2f, 1.0f - (ageInHours * 0.05f))
                }
            }
        } catch (e: Exception) {
            // If date parsing fails, assume it's somewhat old but don't completely penalize it.
            decayMultiplier = 0.5f
        }

        return score * decayMultiplier
    }
}
