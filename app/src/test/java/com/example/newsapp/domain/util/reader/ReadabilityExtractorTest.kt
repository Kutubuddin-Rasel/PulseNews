package com.example.newsapp.domain.util.reader

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadabilityExtractorTest {
    private val extractor = ReadabilityExtractor()

    private fun page(body: String) = """
        <html><head><title>Test Title</title></head><body>
        <nav>Home About <a href="/x">Related Link</a></nav>
        $body
        <footer>© 2026 Example, subscribe to our newsletter</footer>
        </body></html>
    """.trimIndent()

    @Test fun extracts_main_article_and_excludes_chrome() {
        val body = "<article>" + (1..6).joinToString("") {
            "<p>This is a substantial paragraph number $it with enough words to count as real article body text for the reader.</p>"
        } + "</article>"
        val result = extractor.extract(page(body), "https://example.com/a")
        assertNotNull(result)
        result!!
        assertTrue("main content kept", result.contentHtml.contains("substantial paragraph number 1"))
        assertTrue("nav excluded", !result.contentHtml.contains("Related Link"))
        assertTrue("not thin", !result.quality.isThin())
    }

    @Test fun thin_page_reports_thin_quality() {
        val result = extractor.extract(page("<p>Short.</p>"), "https://example.com/b")
        // Either null (Readability found nothing) or a thin quality — both route to WebView.
        if (result != null) assertTrue(result.quality.isThin())
    }

    @Test fun garbage_html_does_not_throw() {
        val result = extractor.extract("<html broken <<", "https://example.com/c")
        assertNull(result) // unparseable → null, caller falls back to WebView
    }
}
