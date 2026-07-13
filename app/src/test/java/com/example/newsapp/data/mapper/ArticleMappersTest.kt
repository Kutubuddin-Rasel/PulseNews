package com.example.newsapp.data.mapper

import com.example.newsapp.data.remote.dto.ArticleDto
import com.example.newsapp.data.remote.dto.PulseArticleDto
import com.example.newsapp.data.remote.dto.SourceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleMappersTest {

    @Test
    fun `toDomainOrNull returns null when url missing`() {
        val dto = ArticleDto(
            author = "A",
            content = "C",
            description = "D",
            publishedAt = "2026-01-01T00:00:00Z",
            source = SourceDto("id", "source"),
            title = "Title",
            url = null,
            urlToImage = null
        )

        val result = dto.toDomainOrNull()

        assertNull(result)
    }

    @Test
    fun `toDomainOrNull maps required fields`() {
        val dto = ArticleDto(
            author = "A",
            content = "C",
            description = "D",
            publishedAt = "2026-01-01T00:00:00Z",
            source = SourceDto("id", "source"),
            title = "Title",
            url = "https://example.com",
            urlToImage = "https://example.com/image.png"
        )

        val result = dto.toDomainOrNull()

        requireNotNull(result)
        assertEquals("https://example.com", result.url)
        assertEquals("Title", result.title)
        assertEquals("source", result.source.name)
    }

    @Test
    fun `PulseArticleDto toDomainOrNull maps summary and gravity (CONF1, CONF2)`() {
        val dto = PulseArticleDto(
            id = "id1",
            title = "Breaking News",
            link = "https://example.com/a",
            snippet = "<p>Snippet</p>",
            pubDate = "2026-06-15",
            source = "News - BBC",
            summary = "<b>Short recap</b>",
            gravity_score = 12.5f
        )

        val article = dto.toDomainOrNull()

        requireNotNull(article)
        assertEquals("https://example.com/a", article.url)
        assertEquals("Short recap", article.summary) // HTML stripped, trimmed
        assertEquals("BBC", article.source.name)      // "News - " prefix removed
        assertEquals(12.5f, article.gravity_score!!, 0.001f)
    }

    @Test
    fun `PulseArticleDto toDomainOrNull returns null when link missing`() {
        val dto = PulseArticleDto(
            id = "1",
            title = "Title",
            link = null,
            snippet = "s",
            pubDate = "2026-06-15",
            source = "BBC"
        )

        assertNull(dto.toDomainOrNull())
    }

    @Test
    fun `PulseArticleDto toDomainOrNull null summary stays null (CONF2)`() {
        val dto = PulseArticleDto(
            id = "1",
            title = "Title",
            link = "https://example.com/b",
            snippet = "s",
            pubDate = "2026-06-15",
            source = "BBC",
            summary = null
        )

        val article = dto.toDomainOrNull()

        requireNotNull(article)
        assertNull(article.summary) // reader will fall back to on-demand /summary
    }
}
