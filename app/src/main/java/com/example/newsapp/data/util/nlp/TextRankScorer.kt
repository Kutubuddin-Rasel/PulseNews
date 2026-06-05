package com.example.newsapp.data.util.nlp

import kotlin.math.abs

class TextRankScorer {

    /**
     * Executes the PageRank algorithm on a graph of sentences.
     * 
     * @param tokensPerSentence A list of token lists, where index corresponds to sentence index.
     * @param similarityCalculator The engine to compute edge weights.
     * @param dampingFactor The probability of continuing the random walk (default 0.85).
     * @param maxIterations Maximum loops before forcefully stopping.
     * @param convergenceThreshold Stop iterating if no score changes by more than this amount.
     * @return A map of sentence index to its calculated TextRank score.
     */
    fun scoreSentences(
        tokensPerSentence: List<List<String>>,
        similarityCalculator: SimilarityCalculator,
        dampingFactor: Float = 0.85f,
        maxIterations: Int = 30,
        convergenceThreshold: Float = 0.0001f
    ): Map<Int, Float> {
        val size = tokensPerSentence.size
        if (size == 0) return emptyMap()
        if (size == 1) return mapOf(0 to 1.0f)

        // Build the weighted graph (adjacency matrix)
        val weights = Array(size) { FloatArray(size) }
        val weightSums = FloatArray(size)

        for (i in 0 until size) {
            for (j in i + 1 until size) {
                val sim = similarityCalculator.calculateSimilarity(tokensPerSentence[i], tokensPerSentence[j])
                weights[i][j] = sim
                weights[j][i] = sim
                weightSums[i] += sim
                weightSums[j] += sim
            }
        }

        // Initialize scores (all nodes start with a score of 1.0)
        var currentScores = FloatArray(size) { 1.0f }
        var nextScores = FloatArray(size)

        // Iterate until convergence or max iterations
        for (iteration in 0 until maxIterations) {
            var maxChange = 0f

            for (i in 0 until size) {
                var sum = 0f
                for (j in 0 until size) {
                    if (i != j && weightSums[j] > 0) {
                        // PR(Vi) = (1-d) + d * Sum( PR(Vj) * Weight(ji) / Sum(Weight(jk)) )
                        sum += currentScores[j] * (weights[j][i] / weightSums[j])
                    }
                }
                
                nextScores[i] = (1 - dampingFactor) + (dampingFactor * sum)
                
                maxChange = maxOf(maxChange, abs(nextScores[i] - currentScores[i]))
            }

            // Swap arrays for next iteration
            val temp = currentScores
            currentScores = nextScores
            nextScores = temp

            // Check for convergence
            if (maxChange < convergenceThreshold) {
                break
            }
        }

        // Map final scores back to their sentence index
        val scoreMap = mutableMapOf<Int, Float>()
        for (i in 0 until size) {
            scoreMap[i] = currentScores[i]
        }
        
        return scoreMap
    }
}
