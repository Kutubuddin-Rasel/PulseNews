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

private val statusMap = VerificationStatus.entries.associateBy { it.name }

fun ArticleDto.toDomainOrNull(): Article? {
    val cleanedUrl = url?.trim().orEmpty()
    val cleanedTitle = title?.trim().orEmpty()
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
                status = statusMap[it.status ?: "UNVERIFIED"] ?: VerificationStatus.UNVERIFIED,
                verificationMethod = it.verificationMethod,
                trustedSigner = it.trustedSigner
            )
        }
    )
}

fun PulseArticleDto.toDomainOrNull(): Article? {
    val cleanedUrl = link?.trim().orEmpty()
    val cleanedTitle = title?.trim().orEmpty().fastStripHtml()
    if (cleanedUrl.isEmpty() || cleanedTitle.isEmpty()) {
        return null
    }

    return Article(
        url = cleanedUrl,
        backendId = id.orEmpty(),
        author = null,
        content = null,
        description = snippet?.trim().orEmpty().fastStripHtml(),
        publishedAt = pubDate.orEmpty(),
        source = Source(
            id = null,
            name = source?.trim().orEmpty().removePrefix("News - ").trim().ifEmpty { "Unknown" }
        ),
        title = cleanedTitle,
        urlToImage = urlToImage,
        provenance = provenance?.let {
            Provenance(
                status = statusMap[it.status ?: "UNVERIFIED"] ?: VerificationStatus.UNVERIFIED,
                verificationMethod = it.verificationMethod,
                trustedSigner = it.trustedSigner
            )
        },
        regionCode = regionCode,
        sourceTier = sourceTier,
        gravity_score = gravity_score,
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
        gravity_score = gravity_score
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
        provenance = Provenance(
            status = statusMap[verificationStatus] ?: VerificationStatus.UNVERIFIED,
            verificationMethod = signatureProtocol,
            trustedSigner = trustedSigner
        ),
        regionCode = regionCode,
        sourceTier = sourceTier,
        gravity_score = gravity_score,
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
