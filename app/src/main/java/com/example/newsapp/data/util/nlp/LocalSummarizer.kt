package com.example.newsapp.data.util.nlp

interface LocalSummarizer {
    /**
     * Extracts a summary from the given text.
     *
     * @param text The full text of the article.
     * @param title The title of the article (optional, used for heuristics).
     * @param maxSentences The maximum number of sentences to extract.
     * @return A formatted summary string (e.g., bullet points).
     */
    fun summarize(text: String, title: String? = null, maxSentences: Int = 3): String
}
