package com.example.newsapp.data.mapper

import com.example.newsapp.Room.CachedFeedArticleEntity
import com.example.newsapp.data.remote.dto.ArticleDto
import com.example.newsapp.data.remote.dto.PulseArticleDto
import com.example.newsapp.data.remote.dto.TrendingTopicDto
import com.example.newsapp.domain.model.Provenance
import com.example.newsapp.domain.model.TrendingTopic
import com.example.newsapp.domain.model.VerificationStatus
import com.example.newsapp.module.Article
import com.example.newsapp.module.ArticleTaxonomy
import com.example.newsapp.module.Source
import androidx.core.text.HtmlCompat
import java.util.regex.Pattern

private val HTML_PATTERN = Pattern.compile("<[^>]*>")
private fun String.fastStripHtml(): String {
    return HTML_PATTERN.matcher(this).replaceAll("")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

// private val statusMap = VerificationStatus.entries.associateBy { it.name } // Removed since we are using valueOf directly

private fun String.cleanSeoSuffixes(): String {
    // Remove everything after the first ` | ` (e.g., "Title | Author | Publisher")
    val pipeIndex = this.indexOf(" | ")
    var cleaned = if (pipeIndex > 0) this.substring(0, pipeIndex) else this

    // Remove typical trailing dash Publisher (e.g., "Title - BBC News").
    // We only do this for the last dash, and only if the text after the dash is short (< 35 chars)
    val lastDashIndex = cleaned.lastIndexOf(" - ")
    if (lastDashIndex > 0 && cleaned.length - lastDashIndex < 35) {
        cleaned = cleaned.substring(0, lastDashIndex)
    }
    
    val lastEmDashIndex = cleaned.lastIndexOf(" — ")
    if (lastEmDashIndex > 0 && cleaned.length - lastEmDashIndex < 35) {
        cleaned = cleaned.substring(0, lastEmDashIndex)
    }

    return cleaned.trim()
}

fun ArticleDto.toDomainOrNull(): Article? {
    val cleanedUrl = url?.trim().orEmpty()
    val cleanedTitle = title?.trim().orEmpty().cleanSeoSuffixes()
    if (cleanedUrl.isEmpty() || cleanedTitle.isEmpty()) {
        return null
    }

    return Article(
        url = cleanedUrl,
        author = author,
        content = content,
        description = description,
        publishedAt = publishedAt,
        source = Source(
            id = source?.id,
            name = source?.name?.trim().orEmpty().ifEmpty { "Unknown" }
        ),
        title = cleanedTitle,
        urlToImage = urlToImage,
        provenance = provenance?.let {
            Provenance(
                status = runCatching { VerificationStatus.valueOf(it.status ?: "UNVERIFIED") }.getOrDefault(VerificationStatus.UNVERIFIED),
                verificationMethod = it.verificationMethod,
                trustedSigner = it.trustedSigner
            )
        }
    )
}

fun PulseArticleDto.toDomainOrNull(): Article? {
    val cleanedUrl = link?.trim().orEmpty()
    val cleanedTitle = title?.trim().orEmpty().fastStripHtml().cleanSeoSuffixes()
    if (cleanedUrl.isEmpty() || cleanedTitle.isEmpty()) {
        return null
    }

    return Article(
        url = cleanedUrl,
        backendId = id.orEmpty(),
        author = author,
        content = null,
        description = snippet?.trim().orEmpty().fastStripHtml(),
        publishedAt = pubDate.orEmpty(),
        source = Source(
            id = null,
            name = source?.trim().orEmpty().removePrefix("News - ").trim().ifEmpty { "Unknown" }
        ),
        title = cleanedTitle,
        urlToImage = urlToImage,
        summary = summary?.trim()?.fastStripHtml()?.ifEmpty { null },
        provenance = provenance?.let {
            Provenance(
                status = runCatching { VerificationStatus.valueOf(it.status ?: "UNVERIFIED") }.getOrDefault(VerificationStatus.UNVERIFIED),
                verificationMethod = it.verificationMethod,
                trustedSigner = it.trustedSigner
            )
        },
        regionCode = regionCode,
        sourceTier = sourceTier,
        gravity_score = gravity_score,
        personalized_score = personalized_score,
        distance = distance,
        taxonomy = taxonomy?.let {
            ArticleTaxonomy(
                categories = it.categories ?: emptyList(),
                tags = it.tags ?: emptyList(),
                mlConfidence = it.mlConfidence,
                id = it.id,
                articleId = it.articleId
            )
        }
    )
}

fun Article.toCacheEntity(feedKey: String, sortOrder: Int, fetchedAt: Long): CachedFeedArticleEntity {
    return CachedFeedArticleEntity(
        feedKey = feedKey,
        backendId = backendId,
        url = url,
        author = author,
        content = content,
        description = description,
        publishedAt = publishedAt,
        sourceId = source.id,
        sourceName = source.name,
        title = title,
        urlToImage = urlToImage,
        summary = summary,
        sortOrder = sortOrder,
        fetchedAt = fetchedAt,
        verificationStatus = provenance?.status?.name ?: "UNVERIFIED",
        signatureProtocol = provenance?.verificationMethod,
        trustedSigner = provenance?.trustedSigner,
        regionCode = regionCode,
        sourceTier = sourceTier,
        taxonomyCategories = taxonomy?.categories,
        taxonomyTags = taxonomy?.tags,
        taxonomyMlConfidence = taxonomy?.mlConfidence,
        taxonomyId = taxonomy?.id,
        taxonomyArticleId = taxonomy?.articleId,
        gravity_score = gravity_score,
        personalizedScore = personalized_score,
        distance = distance
    )
}

fun CachedFeedArticleEntity.toDomainArticle(): Article {
    return Article(
        url = url,
        backendId = backendId,
        author = author,
        content = content,
        description = description,
        publishedAt = publishedAt,
        source = Source(sourceId, sourceName),
        title = title,
        urlToImage = urlToImage,
        summary = summary,
        provenance = Provenance(
            status = runCatching { VerificationStatus.valueOf(verificationStatus) }.getOrDefault(VerificationStatus.UNVERIFIED),
            verificationMethod = signatureProtocol,
            trustedSigner = trustedSigner
        ),
        regionCode = regionCode,
        sourceTier = sourceTier,
        gravity_score = gravity_score,
        personalized_score = personalizedScore,
        distance = distance,
        taxonomy = if (taxonomyCategories != null || taxonomyTags != null) {
            ArticleTaxonomy(
                categories = taxonomyCategories ?: emptyList(),
                tags = taxonomyTags ?: emptyList(),
                mlConfidence = taxonomyMlConfidence,
                id = taxonomyId,
                articleId = taxonomyArticleId
            )
        } else null
    )
}

fun TrendingTopicDto.toDomain(): TrendingTopic {
    return TrendingTopic(
        tag = tag,
        count = count
    )
}
