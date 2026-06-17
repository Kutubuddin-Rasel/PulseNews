package com.example.newsapp.domain.util.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderQualityTest {
    @Test fun thin_when_too_few_chars() {
        assertTrue(ReaderQuality(charCount = 599, paragraphCount = 9).isThin())
    }
    @Test fun thin_when_too_few_paragraphs() {
        assertTrue(ReaderQuality(charCount = 5000, paragraphCount = 1).isThin())
    }
    @Test fun ok_at_threshold_boundary() {
        assertFalse(ReaderQuality(charCount = 600, paragraphCount = 2).isThin())
    }
    @Test fun ok_when_rich() {
        assertFalse(ReaderQuality(charCount = 5000, paragraphCount = 12).isThin())
    }
    @Test fun thresholds_are_the_documented_constants() {
        assertEquals(600, ReaderQuality.MIN_CHARS)
        assertEquals(2, ReaderQuality.MIN_PARAGRAPHS)
    }
}
