package com.example.newsapp.data.mapper

import com.example.newsapp.data.remote.dto.ArticleDto
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
}
