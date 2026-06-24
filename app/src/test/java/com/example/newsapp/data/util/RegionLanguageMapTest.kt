package com.example.newsapp.data.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RegionLanguageMapTest {
    @Test
    fun `english device in germany reads en and de`() {
        assertEquals(listOf("en", "de"), resolveReadableLanguages("DE", "en"))
    }

    @Test
    fun `multilingual region adds all local languages`() {
        assertEquals(listOf("en", "de", "fr", "it"), resolveReadableLanguages("CH", "en"))
    }

    @Test
    fun `dedups when device language equals region language`() {
        assertEquals(listOf("de", "en"), resolveReadableLanguages("DE", "de"))
    }

    @Test
    fun `null or unknown region yields device plus english`() {
        assertEquals(listOf("en"), resolveReadableLanguages(null, "en"))
        assertEquals(listOf("fr", "en"), resolveReadableLanguages("ZZ", "fr"))
    }
}
