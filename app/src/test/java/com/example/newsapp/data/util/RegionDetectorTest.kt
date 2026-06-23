package com.example.newsapp.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionDetectorTest {
    @Test
    fun `network country wins`() {
        assertEquals("DE", pickRegion(network = "de", sim = "fr", locale = "US"))
    }

    @Test
    fun `falls back to sim then locale`() {
        assertEquals("FR", pickRegion(network = "", sim = "fr", locale = "US"))
        assertEquals("US", pickRegion(network = null, sim = " ", locale = "us"))
    }

    @Test
    fun `non 2-letter codes are rejected to null`() {
        assertNull(pickRegion(network = "", sim = "", locale = ""))
        assertNull(pickRegion(network = "USA", sim = "1", locale = "x"))
    }
}
