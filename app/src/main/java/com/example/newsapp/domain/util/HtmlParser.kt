package com.example.newsapp.domain.util

// The article AST. Content isolation moved to ReaderQuality + ReadabilityExtractor +
// ArticleBlockMapper (domain.util.reader); the old `object HtmlParser` regex/Jsoup scraper
// and its `ParsedArticle` wrapper were retired. These block types remain the single shared
// shape the LazyColumn reader renders.

sealed interface TextType {
    data object H1 : TextType
    data object H2 : TextType
    data object H3 : TextType
    data object PARAGRAPH : TextType
}

sealed class ArticleBlock {
    data class Text(val content: String, val type: TextType = TextType.PARAGRAPH) : ArticleBlock()
    data class Image(val url: String, val caption: String? = null) : ArticleBlock()
    data class Video(val url: String, val platform: String) : ArticleBlock()
}
