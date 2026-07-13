package com.example.newsapp.domain.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPreferencesTest {
    @Test fun defaults_are_sane() {
        val p = ReadingPreferences()
        assertEquals(1.0f, p.fontScale, 0.001f)
        assertEquals(LineHeightOption.NORMAL, p.lineHeight)
        assertEquals(WidthOption.MEDIUM, p.measureWidth)
        assertEquals(ReaderTheme.LIGHT, p.theme)
        assertEquals(false, p.bionicEnabled)
        assertEquals(false, p.focusEnabled)
    }
    @Test fun line_height_multipliers() {
        assertEquals(1.3f, LineHeightOption.COMPACT.multiplier, 0.001f)
        assertEquals(1.5f, LineHeightOption.NORMAL.multiplier, 0.001f)
        assertEquals(1.7f, LineHeightOption.RELAXED.multiplier, 0.001f)
    }
}
