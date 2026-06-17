package com.example.newsapp.ui.components.reader

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BionicTextTest {
    @Test fun bolds_leading_fraction_of_each_word() {
        val s = buildBionicString("reading")           // 7 chars -> ceil(7*0.4)=3 bold
        val boldSpans = s.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)
        assertEquals(0, boldSpans[0].start)
        assertEquals(3, boldSpans[0].end)
        assertEquals("reading", s.text)
    }
    @Test fun single_char_word_bolds_one() {
        val s = buildBionicString("a")
        val bold = s.spanStyles.first { it.item.fontWeight == FontWeight.Bold }
        assertEquals(0, bold.start); assertEquals(1, bold.end)
    }
    @Test fun preserves_punctuation_and_spacing() {
        val s = buildBionicString("Hello, world!")
        assertEquals("Hello, world!", s.text)
        assertTrue(s.spanStyles.size >= 2) // one per word
    }
}
