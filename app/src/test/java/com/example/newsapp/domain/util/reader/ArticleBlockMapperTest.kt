package com.example.newsapp.domain.util.reader

import com.example.newsapp.domain.util.ArticleBlock
import com.example.newsapp.domain.util.TextType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleBlockMapperTest {
    private val mapper = ArticleBlockMapper()

    @Test fun maps_headings_paragraphs_images_in_order() {
        val html = """
            <h2>Section</h2>
            <p>This is a real paragraph of article body text that is comfortably long enough.</p>
            <img src="https://cdn.example.com/pic.jpg"/>
        """.trimIndent()
        val blocks = mapper.map(html, title = "Headline", heroImageUrl = null)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ArticleBlock.Text && (blocks[0] as ArticleBlock.Text).type == TextType.H2)
        assertTrue(blocks[1] is ArticleBlock.Text && (blocks[1] as ArticleBlock.Text).type == TextType.PARAGRAPH)
        assertTrue(blocks[2] is ArticleBlock.Image)
    }

    @Test fun drops_short_paragraphs_and_title_echo() {
        val html = "<p>Too short</p><p>Headline</p><p>This is the genuine opening paragraph of the story with plenty of words.</p>"
        val blocks = mapper.map(html, title = "Headline", heroImageUrl = null)
        assertEquals(1, blocks.size)
        assertTrue((blocks[0] as ArticleBlock.Text).content.startsWith("This is the genuine"))
    }

    @Test fun skips_hero_duplicate_image() {
        val hero = "https://cdn.example.com/hero.jpg"
        val html = """<img src="$hero?w=1200"/><p>Body paragraph long enough to be kept by the mapper threshold.</p>"""
        val blocks = mapper.map(html, title = "T", heroImageUrl = hero)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ArticleBlock.Text)
    }
}
