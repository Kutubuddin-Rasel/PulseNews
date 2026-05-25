package com.example.newsapp.domain.repository

import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow

interface SavedArticleRepository {
    fun observeSavedArticles(): Flow<List<Article>>
    suspend fun saveArticle(article: Article)
    suspend fun deleteArticle(article: Article)
    suspend fun isSaved(url: String): Boolean
    suspend fun articleByUrl(url: String): Article?
    suspend fun syncBookmarks()
}
