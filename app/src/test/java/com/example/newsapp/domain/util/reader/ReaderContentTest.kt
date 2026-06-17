package com.example.newsapp.domain.util.reader

import com.example.newsapp.domain.util.ArticleBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderContentTest {
    @Test fun read_time_rounds_up_from_word_count() {
        val blocks = listOf(ArticleBlock.Text(List(440) { "word" }.joinToString(" ")))
        val c = ReaderContent.from(title = "T", heroImageUrl = null, blocks = blocks,
            quality = ReaderQuality(3000, 5), sourceUrl = "https://e.com")
        assertEquals(440, c.wordCount)        // 440 words
        assertEquals(2, c.estReadMinutes)     // ceil(440/220)
    }
    @Test fun minutes_left_tracks_progress() {
        val c = ReaderContent.from("T", null, listOf(ArticleBlock.Text(List(660) { "w" }.joinToString(" "))),
            ReaderQuality(3000, 5), "https://e.com")
        assertEquals(3, c.estReadMinutes)
        assertEquals(2, c.minutesLeft(progress = 0.34f)) // ceil(3 * 0.66)
        assertEquals(0, c.minutesLeft(progress = 1f))
    }
}
