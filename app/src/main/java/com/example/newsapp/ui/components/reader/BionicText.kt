package com.example.newsapp.ui.components.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlin.math.ceil

private val WORD = Regex("\\S+")

/** Bolds the leading ~40% of each word's letters (min 1) for "bionic" reading. */
fun buildBionicString(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in WORD.findAll(text)) {
        append(text.substring(last, m.range.first))      // gap (spaces/newlines)
        val word = m.value
        val leadLetters = word.takeWhile { it.isLetterOrDigit() }.length
        val boldLen = if (leadLetters == 0) 0 else ceil(leadLetters * 0.4).toInt().coerceAtLeast(1)
        if (boldLen > 0) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(word.substring(0, boldLen)) }
            append(word.substring(boldLen))
        } else append(word)
        last = m.range.last + 1
    }
    append(text.substring(last))
}
