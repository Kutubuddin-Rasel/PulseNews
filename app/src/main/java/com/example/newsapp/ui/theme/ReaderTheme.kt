package com.example.newsapp.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.newsapp.domain.reader.ReaderTheme

data class ReaderColors(val background: Color, val text: Color, val secondaryText: Color)

fun readerColorsFor(theme: ReaderTheme): ReaderColors = when (theme) {
    ReaderTheme.LIGHT -> ReaderColors(Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFF4A4A4A))
    ReaderTheme.SEPIA -> ReaderColors(Color(0xFFF4ECD8), Color(0xFF5B4636), Color(0xFF7A6450))
    ReaderTheme.DARK -> ReaderColors(Color(0xFF121212), Color(0xFFE6E6E6), Color(0xFFB0B0B0))
    ReaderTheme.HIGH_CONTRAST -> ReaderColors(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFE0E0E0))
}
