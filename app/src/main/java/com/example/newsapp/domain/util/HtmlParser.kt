package com.example.newsapp.domain.util

import java.util.ArrayDeque

enum class TextType { H1, H2, H3, PARAGRAPH }

sealed class ArticleBlock {
    data class Text(val content: String, val type: TextType = TextType.PARAGRAPH) : ArticleBlock()
    data class Image(val url: String, val caption: String? = null) : ArticleBlock()
    data class Video(val url: String, val platform: String) : ArticleBlock()
}

data class ParsedArticle(
    val title: String,
    val heroImageUrl: String?,
    val blocks: List<ArticleBlock> // Replaced paragraphs with AST blocks
)

object HtmlParser {
    // Tags that should trigger ignoring their inner content
    private val IGNORED_TAGS = setOf("nav", "aside", "footer", "script", "style", "header", "form", "button")
    
    private fun String.normalizeForComparison(): String {
        return this.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
    
    fun parse(html: String): ParsedArticle {
        val document = org.jsoup.Jsoup.parse(html)
        var title = document.title() ?: ""
        var heroImageUrl: String? = document.select("meta[property=og:image]").attr("content")
        if (heroImageUrl?.isBlank() == true) heroImageUrl = document.select("meta[property='og:image']").attr("content")
        if (heroImageUrl?.isBlank() == true) heroImageUrl = null

        val blocks = mutableListOf<ArticleBlock>()
        
        // Remove noise tags
        document.select("nav, aside, footer, script, style, header, form, button, iframe").remove()

        val titleNormalized = title.normalizeForComparison()

        // Try extracting standard tags first
        val elements = document.body().select("p, h1, h2, h3, img[src]")
        elements.forEach { element ->
            when (element.tagName()) {
                "p" -> {
                    val text = element.text().trim()
                    if (text.length > 30) {
                        val textNormalized = text.normalizeForComparison()
                        // Skip if the first paragraph is just repeating the title
                        val isDuplicateTitle = blocks.isEmpty() && textNormalized.length > 15 && 
                            (titleNormalized.contains(textNormalized) || textNormalized.contains(titleNormalized))
                            
                        if (!isDuplicateTitle) {
                            blocks.add(ArticleBlock.Text(text, TextType.PARAGRAPH))
                        }
                    }
                }
                "h1", "h2", "h3" -> { 
                    val text = element.text().trim()
                    if (text.isNotBlank()) {
                        val textNormalized = text.normalizeForComparison()
                        // Check if this header is just repeating the article title
                        val isDuplicateTitle = textNormalized.length > 15 && 
                            (titleNormalized.contains(textNormalized) || textNormalized.contains(titleNormalized))
                        
                        if (!isDuplicateTitle) {
                            val type = when (element.tagName()) {
                                "h1" -> TextType.H1
                                "h2" -> TextType.H2
                                else -> TextType.H3
                            }
                            blocks.add(ArticleBlock.Text(text, type))
                        }
                    } 
                }
                "img" -> {
                    val src = element.attr("src")
                    if (src.startsWith("http")) blocks.add(ArticleBlock.Image(url = src))
                }
            }
        }
        
        // Fallback if no readable paragraphs were found (e.g. site uses only <div>s)
        if (blocks.none { it is ArticleBlock.Text && it.type == TextType.PARAGRAPH }) {
            val text = document.body().text()
            if (text.isNotBlank()) {
                // Split by periods followed by space to create artificial paragraphs
                val chunks = text.split("(?<=\\.)\\s+".toRegex())
                var currentChunk = ""
                for (chunk in chunks) {
                    currentChunk += "$chunk "
                    if (currentChunk.length > 250) {
                        blocks.add(ArticleBlock.Text(currentChunk.trim(), TextType.PARAGRAPH))
                        currentChunk = ""
                    }
                }
                if (currentChunk.isNotBlank()) {
                    blocks.add(ArticleBlock.Text(currentChunk.trim(), TextType.PARAGRAPH))
                }
            }
        }
        
        return ParsedArticle(
            title = title,
            heroImageUrl = heroImageUrl,
            blocks = blocks
        )
    }
}
