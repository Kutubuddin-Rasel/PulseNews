package com.example.newsapp.data.util.nlp

import kotlin.math.log10
import kotlin.math.max

class SimilarityCalculator {

    /**
     * Calculates the similarity between two sentences based on their overlapping tokens.
     * Uses a modified Jaccard/Overlap coefficient normalized by log length to penalize 
     * long sentences that randomly share words, while keeping the score bounds robust.
     * 
     * @param tokens1 Stemmed tokens from sentence 1
     * @param tokens2 Stemmed tokens from sentence 2
     * @return A similarity score between 0.0 and 1.0
     */
    fun calculateSimilarity(tokens1: List<String>, tokens2: List<String>): Float {
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0f

        val set1 = tokens1.toSet()
        val set2 = tokens2.toSet()

        val overlap = set1.intersect(set2).size

        if (overlap == 0) return 0f

        // Logarithmic normalization (standard TextRank formula variation)
        // similarity = |S1 ∩ S2| / (log(|S1|) + log(|S2|))
        // We use max(1.0, ...) to prevent division by zero or negative values.
        val denominator = max(1.0, log10(set1.size.toDouble()) + log10(set2.size.toDouble()))
        
        return (overlap / denominator).toFloat()
    }
}
