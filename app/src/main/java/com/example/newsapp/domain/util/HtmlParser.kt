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
    
    fun parse(html: String): ParsedArticle {
        var title = ""
        var heroImageUrl: String? = null
        val blocks = mutableListOf<ArticleBlock>()
        
        // FSM State
        var inTag = false
        var isClosingTag = false
        var ignoreDepth = 0
        
        val tagNameBuffer = StringBuilder()
        val textBuffer = StringBuilder()
        val attrBuffer = StringBuilder()
        
        // Stack to track tag hierarchy
        val tagStack = ArrayDeque<String>()
        
        var i = 0
        val len = html.length
        
        while (i < len) {
            val c = html[i]
            
            if (!inTag && c == '<') {
                // Entering a tag
                inTag = true
                isClosingTag = false
                tagNameBuffer.setLength(0)
                attrBuffer.setLength(0)
                
                // Check if it's a closing tag
                if (i + 1 < len && html[i + 1] == '/') {
                    isClosingTag = true
                    i++
                } else if (i + 1 < len && html[i + 1] == '!') {
                    // Skip comments <!-- ... -->
                    if (i + 3 < len && html[i + 2] == '-' && html[i + 3] == '-') {
                        val commentEnd = html.indexOf("-->", i + 4)
                        if (commentEnd != -1) {
                            i = commentEnd + 2
                        }
                    } else {
                        // DOCTYPE or similar
                        val end = html.indexOf('>', i)
                        if (end != -1) i = end
                    }
                    inTag = false
                    i++
                    continue
                }
            } else if (inTag && c == '>') {
                // Exiting a tag
                inTag = false
                val tagName = tagNameBuffer.toString().lowercase()
                
                if (isClosingTag) {
                    if (tagStack.isNotEmpty() && tagStack.peek() == tagName) {
                        tagStack.pop()
                    }
                    if (IGNORED_TAGS.contains(tagName) && ignoreDepth > 0) {
                        ignoreDepth--
                    }
                    
                    // Flush text if block closes
                    if (ignoreDepth == 0 && textBuffer.isNotBlank()) {
                        when (tagName) {
                            "p" -> {
                                val text = textBuffer.toString().trim()
                                if (text.length > 30) {
                                    blocks.add(ArticleBlock.Text(text, TextType.PARAGRAPH))
                                }
                                textBuffer.setLength(0)
                            }
                            "h1" -> { blocks.add(ArticleBlock.Text(textBuffer.toString().trim(), TextType.H1)); textBuffer.setLength(0) }
                            "h2" -> { blocks.add(ArticleBlock.Text(textBuffer.toString().trim(), TextType.H2)); textBuffer.setLength(0) }
                            "h3" -> { blocks.add(ArticleBlock.Text(textBuffer.toString().trim(), TextType.H3)); textBuffer.setLength(0) }
                        }
                    }
                } else {
                    if (!tagName.endsWith("/")) {
                        tagStack.push(tagName)
                        if (IGNORED_TAGS.contains(tagName)) {
                            ignoreDepth++
                        }
                    }
                    
                    // Process attributes for the opening tag
                    val attrs = attrBuffer.toString()
                    if (tagName == "meta") {
                        if (attrs.contains("property=\"og:title\"") || attrs.contains("property='og:title'")) {
                            title = extractAttr(attrs, "content") ?: title
                        }
                        if (attrs.contains("property=\"og:image\"") || attrs.contains("property='og:image'")) {
                            heroImageUrl = extractAttr(attrs, "content") ?: heroImageUrl
                        }
                    } else if (tagName == "title" && title.isEmpty()) {
                        // Title will be captured as text, handle special case if needed.
                    } else if (tagName == "img" && ignoreDepth == 0) {
                        val src = extractAttr(attrs, "src")
                        if (src != null && src.startsWith("http")) {
                            blocks.add(ArticleBlock.Image(url = src))
                        }
                    } else if (tagName == "iframe" && ignoreDepth == 0) {
                        val src = extractAttr(attrs, "src")
                        if (src != null && src.contains("youtube")) {
                            blocks.add(ArticleBlock.Video(url = src, platform = "YouTube"))
                        }
                    }
                }
            } else if (inTag) {
                // Collect tag name or attributes
                if (c == ' ' && tagNameBuffer.isNotEmpty() && attrBuffer.isEmpty()) {
                    // Transition from tag name to attributes
                    attrBuffer.append(' ')
                } else if (attrBuffer.isNotEmpty() || (c == ' ' && tagNameBuffer.isNotEmpty())) {
                    attrBuffer.append(c)
                } else if (c != ' ') {
                    tagNameBuffer.append(c)
                }
            } else {
                // Collect text data if not ignored
                if (ignoreDepth == 0) {
                    textBuffer.append(c)
                }
            }
            i++
        }
        
        return ParsedArticle(
            title = title,
            heroImageUrl = heroImageUrl,
            blocks = blocks
        )
    }

    private fun extractAttr(attrs: String, attrName: String): String? {
        val search = "$attrName="
        val start = attrs.indexOf(search)
        if (start == -1) return null
        
        var valStart = start + search.length
        if (valStart >= attrs.length) return null
        
        val quote = attrs[valStart]
        if (quote == '"' || quote == '\'') {
            valStart++
            val end = attrs.indexOf(quote, valStart)
            if (end != -1) {
                return attrs.substring(valStart, end)
            }
        }
        return null
    }
}
