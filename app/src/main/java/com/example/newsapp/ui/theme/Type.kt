package com.example.newsapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.example.newsapp.R

fun TextStyle.nonLinearScale(density: Density, maxScale: Float): TextStyle {
    val scale = density.fontScale
    if (scale <= 1f) return this
    val ratio = scale.coerceAtMost(maxScale) / scale
    return this.copy(fontSize = fontSize * ratio, lineHeight = lineHeight * ratio)
}

// Editorial serif — Newsreader (variable, optical sizing)
val Newsreader = FontFamily(
    Font(R.font.newsreader_regular,  FontWeight.Normal),
    Font(R.font.newsreader_medium,   FontWeight.Medium),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold),
    Font(R.font.newsreader_bold,     FontWeight.Bold),
    Font(R.font.newsreader_medium_italic, FontWeight.Medium, androidx.compose.ui.text.font.FontStyle.Italic)
)

// UI sans — Geist
val Geist = FontFamily(
    Font(R.font.geist_regular,  FontWeight.Normal),
    Font(R.font.geist_medium,   FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold,     FontWeight.Bold)
)

// Metadata mono — Geist Mono
val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium,  FontWeight.Medium)
)

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 57.sp, lineHeight = 60.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 45.sp, lineHeight = 48.sp, letterSpacing = (-0.4).sp),
    displaySmall  = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.3).sp),
    headlineLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.25).sp),
    headlineMedium= TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.15).sp),
    titleLarge    = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium   = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    titleSmall    = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.05.sp),
    bodyLarge     = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium    = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    bodySmall     = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelLarge    = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall    = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// Public extra style for source/timestamp metadata
val MetaMono = TextStyle(
    fontFamily = GeistMono, fontWeight = FontWeight.Medium,
    fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp
)