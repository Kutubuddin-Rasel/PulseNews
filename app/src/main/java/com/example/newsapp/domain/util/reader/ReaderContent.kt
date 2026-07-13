package com.example.newsapp.domain.util.reader

import com.example.newsapp.domain.util.ArticleBlock
import kotlin.math.ceil

data class ReaderContent(
    val title: String,
    val heroImageUrl: String?,
    val blocks: List<ArticleBlock>,
    val quality: ReaderQuality,
    val wordCount: Int,
    val estReadMinutes: Int,
    val sourceUrl: String,
) {
    fun minutesLeft(progress: Float): Int =
        ceil(estReadMinutes * (1f - progress.coerceIn(0f, 1f))).toInt()

    companion object {
        private const val WORDS_PER_MINUTE = 220
        fun from(title: String, heroImageUrl: String?, blocks: List<ArticleBlock>,
                 quality: ReaderQuality, sourceUrl: String): ReaderContent {
            val words = blocks.filterIsInstance<ArticleBlock.Text>()
                .sumOf { it.content.trim().split(Regex("\\s+")).count { w -> w.isNotBlank() } }
            return ReaderContent(title, heroImageUrl, blocks, quality, words,
                ceil(words.toDouble() / WORDS_PER_MINUTE).toInt().coerceAtLeast(1), sourceUrl)
        }
    }
}
