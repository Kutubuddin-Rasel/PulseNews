package com.example.newsapp.domain.util

import org.jsoup.Jsoup

data class ParsedArticle(
    val title: String,
    val heroImageUrl: String?,
    val paragraphs: List<String>
)

object HtmlParser {
    fun parse(html: String): ParsedArticle {
        val doc = Jsoup.parse(html)
        
        // Find title
        val title = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
            ?: doc.title()

        // Find hero image
        val heroImageUrl = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
        
        // Find main article container heuristically
        val articleNode = doc.select("article").firstOrNull() ?: doc.body()
        
        // Strip out noisy tags
        articleNode.select("nav, aside, footer, script, style, header, iframe, form, button").remove()
        
        // Extract paragraphs that actually contain meaningful text
        val paragraphs = articleNode.select("p")
            .map { it.text() }
            .filter { it.isNotBlank() && it.length > 30 } // Filter out short snippets like "Share this" or "By Author"
            
        return ParsedArticle(
            title = title,
            heroImageUrl = heroImageUrl,
            paragraphs = paragraphs
        )
    }
}
