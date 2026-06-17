package com.example.newsapp.domain.util.reader

import com.example.newsapp.domain.util.ArticleBlock
import com.example.newsapp.domain.util.TextType
import org.jsoup.Jsoup

/** Converts already-isolated article HTML (from ReadabilityExtractor) into the
 * ArticleBlock AST the reader renders. */
class ArticleBlockMapper {
    private fun String.norm() = lowercase().replace(Regex("[^a-z0-9]"), "")

    fun map(contentHtml: String, title: String, heroImageUrl: String?): List<ArticleBlock> {
        val doc = Jsoup.parse(contentHtml)
        val titleN = title.norm()
        val heroBase = heroImageUrl?.substringBefore("?")
        val blocks = mutableListOf<ArticleBlock>()
        for (el in doc.body().select("p, h1, h2, h3, img[src]")) {
            when (el.tagName()) {
                "p" -> {
                    val text = el.text().trim()
                    if (text.length <= 30) continue
                    // Echo dedup only fires when both the title and the text are substantial,
                    // so a short title (e.g. "T") can't trivially match real body text by substring.
                    val isTitleEcho = blocks.isEmpty() && text.norm().length > 15 && titleN.length > 15 &&
                        (titleN.contains(text.norm()) || text.norm().contains(titleN))
                    if (!isTitleEcho) blocks.add(ArticleBlock.Text(text, TextType.PARAGRAPH))
                }
                "h1", "h2", "h3" -> {
                    val text = el.text().trim()
                    if (text.isBlank()) continue
                    val isTitleEcho = text.norm().length > 15 && titleN.length > 15 &&
                        (titleN.contains(text.norm()) || text.norm().contains(titleN))
                    if (!isTitleEcho) {
                        val type = when (el.tagName()) { "h1" -> TextType.H1; "h2" -> TextType.H2; else -> TextType.H3 }
                        blocks.add(ArticleBlock.Text(text, type))
                    }
                }
                "img" -> {
                    val src = el.attr("src")
                    if (!src.startsWith("http")) continue
                    if (heroBase != null && src.substringBefore("?") == heroBase) continue
                    blocks.add(ArticleBlock.Image(url = src))
                }
            }
        }
        return blocks
    }
}
