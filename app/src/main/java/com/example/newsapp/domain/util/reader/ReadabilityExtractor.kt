package com.example.newsapp.domain.util.reader

import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup

/** Cleaned-content result: the isolated article HTML plus its quality signal. */
data class ExtractionResult(val contentHtml: String, val title: String?, val quality: ReaderQuality)

/** Isolates the main article from a full HTML page using Readability4J (a Jsoup-based
 * Mozilla Readability port). Returns null when nothing usable could be parsed — the
 * caller then falls back to the in-app WebView. */
class ReadabilityExtractor {
    fun extract(html: String, url: String): ExtractionResult? {
        return try {
            val article = Readability4J(url, html).parse()
            val contentHtml = article.content?.takeIf { it.isNotBlank() } ?: return null
            val text = article.textContent.orEmpty()
            val paragraphs = Jsoup.parse(contentHtml).select("p").size
            ExtractionResult(
                contentHtml = contentHtml,
                title = article.title,
                quality = ReaderQuality(charCount = text.trim().length, paragraphCount = paragraphs),
            )
        } catch (e: Exception) {
            null
        }
    }
}
