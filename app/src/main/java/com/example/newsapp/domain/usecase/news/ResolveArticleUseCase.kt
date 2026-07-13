package com.example.newsapp.domain.usecase.news

import com.example.newsapp.domain.usecase.saved.GetSavedArticleUseCase
import com.example.newsapp.module.Article
import javax.inject.Inject

/**
 * D-c/W7: the single source of truth for "resolve the Article behind this url" used by both the
 * reader (WebScreenViewModel) and the detail screen (ArticleDetailViewModel). Prefers the user's
 * saved copy, then falls back to the feed cache. Centralising the fall-through policy here removes
 * the duplicated `getSaved(...) ?: getCached(...)` snippet from both ViewModels (DRY) and gives the
 * resolution one place to evolve (SRP) — e.g. adding a network fetch as a third tier later.
 */
class ResolveArticleUseCase @Inject constructor(
    private val getSavedArticleUseCase: GetSavedArticleUseCase,
    private val getCachedArticleUseCase: GetCachedArticleUseCase
) {
    suspend operator fun invoke(url: String): Article? =
        getSavedArticleUseCase(url) ?: getCachedArticleUseCase(url)
}
