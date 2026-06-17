package com.example.newsapp.domain.util.reader

/** Cheap signal of whether extraction produced enough to read. Below either
 * threshold we route to the WebView fallback instead of the block reader. */
data class ReaderQuality(val charCount: Int, val paragraphCount: Int) {
    fun isThin(): Boolean = charCount < MIN_CHARS || paragraphCount < MIN_PARAGRAPHS

    companion object {
        const val MIN_CHARS = 600
        const val MIN_PARAGRAPHS = 2
    }
}
