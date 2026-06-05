package com.example.newsapp.data.util

import java.util.PriorityQueue

object ExtractiveSummarizer {

    private val STOP_WORDS = setOf(
        "the", "and", "to", "of", "a", "in", "is", "that", "it", "for", "as", "on", "with",
        "was", "by", "this", "at", "are", "be", "from", "or", "an", "they", "which", "you",
        "has", "had", "not", "but", "have", "all", "their", "one", "more", "about", "who",
        "would", "can", "when", "if", "there", "we", "what", "so", "up", "out", "if", "about",
        "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no",
        "just", "him", "know", "take", "people", "into", "year", "your", "good", "some",
        "could", "them", "see", "other", "than", "then", "now", "look", "only", "come", "its",
        "over", "think", "also", "back", "after", "use", "two", "how", "our", "work", "first",
        "well", "way", "even", "new", "want", "because", "any", "these", "give", "day", "most", "us"
    )

    fun extractSummary(text: String, sentenceCount: Int = 3): String {
        if (text.isBlank()) return ""

        // 1. Split into sentences
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.length > 10 }
        if (sentences.size <= sentenceCount) return text

        // 2. Tokenize words and count frequencies
        val wordFrequencies = HashMap<String, Int>()
        val wordRegex = Regex("\\b\\w+\\b")
        
        wordRegex.findAll(text.lowercase()).forEach { match ->
            val word = match.value
            if (word.length > 2 && !STOP_WORDS.contains(word) && !word.matches(Regex("\\d+"))) {
                wordFrequencies[word] = wordFrequencies.getOrDefault(word, 0) + 1
            }
        }

        // Normalize frequencies
        val maxFreq = wordFrequencies.values.maxOrNull()?.toFloat() ?: 1f

        // 3. Score sentences based on word frequencies
        val sentenceScores = HashMap<String, Float>()
        for (sentence in sentences) {
            var score = 0f
            var wordsInSentence = 0
            wordRegex.findAll(sentence.lowercase()).forEach { match ->
                val word = match.value
                val freq = wordFrequencies[word]
                if (freq != null) {
                    score += freq / maxFreq
                }
                wordsInSentence++
            }
            // Normalize by sentence length to prevent bias towards long sentences
            if (wordsInSentence > 0) {
                sentenceScores[sentence] = score / wordsInSentence
            }
        }

        // 4. Extract top sentences, maintaining original order
        val indexedSentences = sentences.mapIndexed { index, sentence ->
            Triple(index, sentence, sentenceScores[sentence] ?: 0f)
        }

        return indexedSentences
            .sortedByDescending { it.third }
            .take(sentenceCount)
            .sortedBy { it.first } // Restore original order
            .joinToString("\n") { "• ${it.second}" }
    }
}
