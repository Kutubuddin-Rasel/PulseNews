package com.example.newsapp.data.util.nlp

class TextPreprocessor {

    companion object {
        // Expanded stop words list for better NLP filtering
        private val STOP_WORDS = setOf(
            "the", "and", "to", "of", "a", "in", "is", "that", "it", "for", "as", "on", "with",
            "was", "by", "this", "at", "are", "be", "from", "or", "an", "they", "which", "you",
            "has", "had", "not", "but", "have", "all", "their", "one", "more", "about", "who",
            "would", "can", "when", "if", "there", "we", "what", "so", "up", "out", "get",
            "go", "me", "make", "like", "time", "no", "just", "him", "know", "take", "people",
            "into", "year", "your", "good", "some", "could", "them", "see", "other", "than",
            "then", "now", "look", "only", "come", "its", "over", "think", "also", "back",
            "after", "use", "two", "how", "our", "work", "first", "well", "way", "even",
            "new", "want", "because", "any", "these", "give", "day", "most", "us", "he", "she",
            "been", "much", "many", "those", "very", "where", "why", "such", "through", "before"
        )
    }

    /**
     * Splits text into a list of sentences.
     */
    fun extractSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        // Split by punctuation followed by whitespace, then trim. Filter out very short segments.
        return text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 15 } // Filter out titles, dates, or artifacts masquerading as sentences
    }

    /**
     * Extracts meaningful, normalized tokens (words) from a sentence.
     */
    fun extractTokens(sentence: String): List<String> {
        val wordRegex = Regex("\\b[a-zA-Z]+\\b")
        return wordRegex.findAll(sentence.lowercase())
            .map { it.value }
            .filter { word ->
                word.length > 2 && !STOP_WORDS.contains(word)
            }
            .map { applyBasicStemming(it) }
            .toList()
    }

    /**
     * Extremely basic stemming to normalize plural nouns and present participles
     * without adding an external dependency like Snowball/Porter.
     */
    private fun applyBasicStemming(word: String): String {
        if (word.length <= 4) return word
        
        var stemmed = word
        if (stemmed.endsWith("ing")) {
            stemmed = stemmed.removeSuffix("ing")
        } else if (stemmed.endsWith("ies")) {
            stemmed = stemmed.removeSuffix("ies") + "y"
        } else if (stemmed.endsWith("es")) {
            stemmed = stemmed.removeSuffix("es")
        } else if (stemmed.endsWith("s") && !stemmed.endsWith("ss")) {
            stemmed = stemmed.removeSuffix("s")
        } else if (stemmed.endsWith("ed")) {
            stemmed = stemmed.removeSuffix("ed")
        }
        
        // Handle double consonants resulting from stripping 'ing' or 'ed' (e.g., 'running' -> 'runn')
        if (stemmed.length > 3 && stemmed.last() == stemmed[stemmed.length - 2] && stemmed.last() != 's') {
            stemmed = stemmed.dropLast(1)
        }
        
        return stemmed
    }
}
