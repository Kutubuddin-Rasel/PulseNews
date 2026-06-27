package com.example.newsapp.data.remote.dto

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PulseArticleDtoTest {

    // Plain Moshi resolves the generated PulseArticleDtoJsonAdapter via @JsonClass — no reflection.
    private val adapter = Moshi.Builder().build().adapter(PulseArticleDto::class.java)

    @Test
    fun `deserialize PulseArticleDto with missing keys leaves nullable fields null`() {
        val json = """
            {
                "id": "1",
                "title": "Test Title"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        // link is nullable with no default; an absent key deserializes to null instead of throwing.
        assertNull(dto.link)
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

        val dto = adapter.fromJson(json)!!

        assertNotNull(dto.gravity_score)
        assertEquals(42.5f, dto.gravity_score!!, 0.001f)
    }

    @Test
    fun `legacy gravity_score json key no longer binds (CONF1)`() {
        // Documents the contract switch: only currentGravityScore is read now; the old key is an
        // unknown field Moshi ignores.
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

        val dto = adapter.fromJson(json)!!

        assertNull(dto.gravity_score)
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

        val dto = adapter.fromJson(json)!!

        assertEquals("A short recap.", dto.summary)
    }

    @Test
    fun `opaque feed cursor is parsed from the payload`() {
        val json = """
            {
                "id": "1",
                "title": "T",
                "cursor": "CUR123"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertEquals("CUR123", dto.cursor)
    }

    @Test
    fun `absent cursor leaves the field null (additive, backward compatible)`() {
        // Older backend responses carry no cursor; the field is nullable so they still parse.
        val json = """
            {
                "id": "1",
                "title": "T"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertNull(dto.cursor)
    }
}
