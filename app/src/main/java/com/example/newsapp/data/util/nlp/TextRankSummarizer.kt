package com.example.newsapp.data.util.nlp

class TextRankSummarizer(
    private val preprocessor: TextPreprocessor = TextPreprocessor(),
    private val similarityCalculator: SimilarityCalculator = SimilarityCalculator(),
    private val textRankScorer: TextRankScorer = TextRankScorer(),
    private val heuristicEngine: HeuristicEngine = HeuristicEngine(preprocessor)
) : LocalSummarizer {

    override fun summarize(text: String, title: String?, maxSentences: Int): String {
        if (text.isBlank()) return ""

        // 1. Extract raw sentences
        val sentences = preprocessor.extractSentences(text)
        if (sentences.size <= maxSentences) {
            // Document is already short enough, just return it as bullet points
            return sentences.joinToString("\n") { "• $it" }
        }

        // 2. Tokenize and clean each sentence
        val tokensPerSentence = sentences.map { preprocessor.extractTokens(it) }

        // 3. Mathematical TextRank Scoring
        val rawScores = textRankScorer.scoreSentences(tokensPerSentence, similarityCalculator)

        // 4. Apply Reading Heuristics (Title overlap, positional bias)
        val finalScores = heuristicEngine.applyHeuristics(rawScores, tokensPerSentence, title)

        // 5. Extract top sentences, maintaining original chronological order
        val indexedSentences = sentences.mapIndexed { index, sentence ->
            Triple(index, sentence, finalScores[index] ?: 0f)
        }

        return indexedSentences
            .sortedByDescending { it.third } // Sort by highest score
            .take(maxSentences)              // Take top N
            .sortedBy { it.first }           // Re-sort by original index to preserve narrative flow
            .joinToString("\n") { "• ${it.second}" }
    }
}
