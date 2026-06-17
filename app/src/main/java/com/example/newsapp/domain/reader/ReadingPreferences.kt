package com.example.newsapp.domain.reader

enum class LineHeightOption(val multiplier: Float) { COMPACT(1.3f), NORMAL(1.5f), RELAXED(1.7f) }
enum class WidthOption(val maxContentWidthDp: Int) { NARROW(540), MEDIUM(640), FULL(Int.MAX_VALUE) }
enum class ReaderTheme { LIGHT, SEPIA, DARK, HIGH_CONTRAST }

data class ReadingPreferences(
    val fontScale: Float = 1.0f,            // clamp 0.85..1.6 at the setter boundary
    val lineHeight: LineHeightOption = LineHeightOption.NORMAL,
    val measureWidth: WidthOption = WidthOption.MEDIUM,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val bionicEnabled: Boolean = false,
    val focusEnabled: Boolean = false,
) {
    companion object { const val MIN_FONT_SCALE = 0.85f; const val MAX_FONT_SCALE = 1.6f }
}
