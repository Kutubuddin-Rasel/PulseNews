package com.example.newsapp.data.repository

import com.example.newsapp.Room.ArticleDatabase
import com.example.newsapp.data.mapper.toCacheEntity
import com.example.newsapp.module.Article
import javax.inject.Inject

/**
 * S3: owns persistence of search results to the local feed cache, kept separate from the paging
 * concern. [SearchPagingSource] produces pages; this caches them so the article-detail screen can
 * resolve a tapped search result offline.
 *
 * F1: bounds the `feedKey = "search"` partition to a single query. The first page of a new search
 * (`page == 1`, a fresh PagingSource) clears the previous query's rows before writing, so the
 * search cache holds at most one query's results instead of accumulating across every search.
 */
class SearchResultCache @Inject constructor(
    private val database: ArticleDatabase
) {
    suspend fun cache(articles: List<Article>, page: Int, pageSize: Int) {
        val fetchedAt = System.currentTimeMillis()
        val entities = articles.mapIndexed { index, article ->
            article.toCacheEntity(SEARCH_FEED_KEY, (page - 1) * pageSize + index, fetchedAt)
        }
        if (page == 1) {
            database.cachedFeedDao().clearFeed(SEARCH_FEED_KEY)
        }
        database.cachedFeedDao().upsertAll(entities)
    }

    companion object {
        const val SEARCH_FEED_KEY = "search"
    }
}
