package com.example.newsapp.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertThrows
import org.junit.Test

class PulseArticleDtoTest {

    @Test
    fun `deserialize PulseArticleDto with missing keys throws NullPointerException`() {
        // Missing required non-nullable fields like link, snippet, pubDate, source
        val json = """
            {
                "id": "1",
                "title": "Test Title"
            }
        """.trimIndent()

        val gson = Gson()
        val dto = gson.fromJson(json, PulseArticleDto::class.java)

        // With the fix, dto.link is properly nullable, so it will be null instead of throwing an unexpected NPE later.
        org.junit.Assert.assertNull(dto.link)
    }

    @Test
    fun `currentGravityScore json key binds to gravity_score field (CONF1)`() {
        val json = """
            {
                "id": "1",
                "title": "T",
                "link": "https://example.com",
                "snippet": "s",
                "pubDate": "2026-06-15",
                "source": "BBC",
                "currentGravityScore": 42.5
            }
        """.trimIndent()

        val dto = Gson().fromJson(json, PulseArticleDto::class.java)

        org.junit.Assert.assertNotNull(dto.gravity_score)
        org.junit.Assert.assertEquals(42.5f, dto.gravity_score!!, 0.001f)
    }

    @Test
    fun `legacy gravity_score json key no longer binds (CONF1)`() {
        // Documents the contract switch: only currentGravityScore is read now.
        val json = """
            {
                "id": "1",
                "title": "T",
                "link": "https://example.com",
                "snippet": "s",
                "pubDate": "2026-06-15",
                "source": "BBC",
                "gravity_score": 9.0
            }
        """.trimIndent()

        val dto = Gson().fromJson(json, PulseArticleDto::class.java)

        org.junit.Assert.assertNull(dto.gravity_score)
    }

    @Test
    fun `summary field is parsed from feed payload (CONF2)`() {
        val json = """
            {
                "id": "1",
                "title": "T",
                "link": "https://example.com",
                "snippet": "s",
                "pubDate": "2026-06-15",
                "source": "BBC",
                "summary": "A short recap."
            }
        """.trimIndent()

        val dto = Gson().fromJson(json, PulseArticleDto::class.java)

        org.junit.Assert.assertEquals("A short recap.", dto.summary)
    }
}
