package com.example.newsapp.data.mapper

import com.example.newsapp.Room.CachedFeedArticleEntity
import com.example.newsapp.data.remote.dto.ArticleDto
import com.example.newsapp.module.Article
import com.example.newsapp.module.Source

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
            com.example.newsapp.domain.model.Provenance(
                status = try { com.example.newsapp.domain.model.VerificationStatus.valueOf(it.status ?: "UNVERIFIED") } catch(e: Exception) { com.example.newsapp.domain.model.VerificationStatus.UNVERIFIED },
                verificationMethod = it.verificationMethod,
                trustedSigner = it.trustedSigner
            )
        }
    )
}

fun com.example.newsapp.data.remote.dto.PulseArticleDto.toDomainOrNull(): Article? {
    val cleanedUrl = link.trim()
    val cleanedTitle = title.trim()
    if (cleanedUrl.isEmpty() || cleanedTitle.isEmpty()) {
        return null
    }

    return Article(
        url = cleanedUrl,
        author = null,
        content = null,
        description = snippet,
        publishedAt = pubDate,
        source = Source(
            id = null,
            name = source.trim().ifEmpty { "Unknown" }
        ),
        title = cleanedTitle,
        urlToImage = null,
        provenance = provenance?.let {
            com.example.newsapp.domain.model.Provenance(
                status = try { com.example.newsapp.domain.model.VerificationStatus.valueOf(it.status ?: "UNVERIFIED") } catch(e: Exception) { com.example.newsapp.domain.model.VerificationStatus.UNVERIFIED },
                verificationMethod = it.verificationMethod,
                trustedSigner = it.trustedSigner
            )
        }
    )
}

fun Article.toCacheEntity(feedKey: String, page: Int, sortOrder: Int, fetchedAt: Long): CachedFeedArticleEntity {
    return CachedFeedArticleEntity(
        feedKey = feedKey,
        page = page,
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
        trustedSigner = provenance?.trustedSigner
    )
}

fun CachedFeedArticleEntity.toDomainArticle(): Article {
    return Article(
        url = url,
        author = author,
        content = content,
        description = description,
        publishedAt = publishedAt,
        source = Source(sourceId, sourceName),
        title = title,
        urlToImage = urlToImage,
        provenance = com.example.newsapp.domain.model.Provenance(
            status = try { com.example.newsapp.domain.model.VerificationStatus.valueOf(verificationStatus) } catch(e: Exception) { com.example.newsapp.domain.model.VerificationStatus.UNVERIFIED },
            verificationMethod = signatureProtocol,
            trustedSigner = trustedSigner
        )
    )
}
