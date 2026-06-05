package com.example.newsapp.domain.util

import com.example.newsapp.module.Article
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

import javax.inject.Inject

class FeedScorer @Inject constructor(
    private val keywordMatcher: KeywordMatcher
) {

    private val TRUSTED_SOURCES = listOf("reuters", "ap", "associated press", "bbc", "npr", "bloomberg")

    companion object {
        private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                return format
            }
        }
    }

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
        return computeScore(
            title = article.title,
            content = article.content,
            description = article.description,
            sourceName = article.source.name,
            sourceTier = article.sourceTier,
            publishedAt = article.publishedAt,
            userWeights = userWeights,
            currentTimeMillis = currentTimeMillis
        )
    }

    /**
     * Overloaded method to compute score using primitive components,
     * preventing allocation of full domain Article and nested objects.
     */
    fun computeScore(
        title: String,
        content: String?,
        description: String?,
        sourceName: String,
        sourceTier: Int?,
        publishedAt: String?,
        userWeights: Map<String, Float>,
        currentTimeMillis: Long
    ): Float {
        var score = 0f

        val titleLower = title.lowercase(Locale.getDefault())
        val contentLower = content?.lowercase(Locale.getDefault()) ?: ""
        val descriptionLower = description?.lowercase(Locale.getDefault()) ?: ""

        val fullText = "$titleLower $contentLower $descriptionLower"

        // 1 & 2. Topic Affinity & Keyword Frequency
        // Uses the Aho-Corasick O(C) matching engine to find frequencies without allocations
        val topicFrequencies = keywordMatcher.matchFrequencies(fullText)

        topicFrequencies.forEach { (topic, matchCount) ->
            // Default weight is 0.5f if the user hasn't explicitly set it yet
            val userWeight = userWeights[topic] ?: 0.5f 
            
            // Apply the user's explicit slider weight
            score += (matchCount * userWeight)
        }

        // Title matches are extremely strong indicators (weight 3x). 
        // We do a secondary pass on just the title for the strong signal multiplier.
        // The engine finds all matches in the title, and we add an extra 2x weight (since 1x was already added above).
        val titleFrequencies = keywordMatcher.matchFrequencies(titleLower)
        titleFrequencies.forEach { (topic, matchCount) ->
            val userWeight = userWeights[topic] ?: 0.5f 
            score += (matchCount * userWeight * 2.0f)
        }

        // 3. Source Trustworthiness
        val sourceLower = sourceName.lowercase(Locale.getDefault())
        if (TRUSTED_SOURCES.any { sourceLower.contains(it) }) {
            score += 2.0f // Flat bonus for highly trusted wire services
        }
        
        // Boost Tier 1 / Tier 2 sources specifically based on backend taxonomy mapping
        when (sourceTier) {
            1 -> score += 3.0f // Massive bonus for top-tier sources
            2 -> score += 1.5f // Good bonus for tier 2
            3 -> score -= 1.0f // Slight penalty for tier 3 if they aren't directly matched
        }

        // 4. Recency Decay
        // News loses relevance. We apply a decay multiplier based on age.
        var decayMultiplier = 1.0f
        try {
            val publishedDate = publishedAt?.let { dateFormat.get()?.parse(it) }
            
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
