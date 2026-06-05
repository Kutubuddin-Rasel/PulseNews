package com.example.newsapp.data.util.nlp

class HeuristicEngine(private val preprocessor: TextPreprocessor) {

    /**
     * Applies reading heuristics to the raw mathematical TextRank scores.
     * 
     * @param rawScores Map of sentence index to its TextRank score.
     * @param tokensPerSentence List of stemmed tokens per sentence.
     * @param title The article title, if available.
     * @return A map of sentence index to its adjusted heuristic score.
     */
    fun applyHeuristics(
        rawScores: Map<Int, Float>,
        tokensPerSentence: List<List<String>>,
        title: String?
    ): Map<Int, Float> {
        if (rawScores.isEmpty()) return rawScores

        val adjustedScores = rawScores.toMutableMap()
        val totalSentences = tokensPerSentence.size

        // 1. Title Overlap Heuristic
        // Sentences that share significant keywords with the headline are usually very important.
        val titleTokens = if (!title.isNullOrBlank()) {
            preprocessor.extractTokens(title).toSet()
        } else {
            emptySet()
        }

        if (titleTokens.isNotEmpty()) {
            for (i in 0 until totalSentences) {
                val sentenceTokens = tokensPerSentence[i].toSet()
                val overlap = titleTokens.intersect(sentenceTokens).size
                if (overlap > 0) {
                    // Boost score based on how many title keywords it contains
                    val boostFactor = 1.0f + (0.2f * overlap)
                    adjustedScores[i] = (adjustedScores[i] ?: 0f) * boostFactor
                }
            }
        }

        // 2. Positional Heuristic
        // The first few sentences of a news article often summarize the core event (inverted pyramid).
        // The very last sentence often concludes or provides the final critical context.
        if (totalSentences > 3) {
            // Give a 30% boost to the first sentence
            adjustedScores[0] = (adjustedScores[0] ?: 0f) * 1.30f
            
            // Give a 15% boost to the second sentence
            adjustedScores[1] = (adjustedScores[1] ?: 0f) * 1.15f
            
            // Give a 15% boost to the absolute last sentence
            val lastIndex = totalSentences - 1
            adjustedScores[lastIndex] = (adjustedScores[lastIndex] ?: 0f) * 1.15f
        }

        return adjustedScores
    }
}
