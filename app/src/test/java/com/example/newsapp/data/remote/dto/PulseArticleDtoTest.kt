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
}
