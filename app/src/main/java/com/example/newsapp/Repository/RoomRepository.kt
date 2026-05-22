package com.example.newsapp.Repository

import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Deprecated("Use domain.repository.SavedArticleRepository directly")
class RoomRepository @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) {
    fun allArticle(): Flow<List<Article>> = savedArticleRepository.observeSavedArticles()
    suspend fun upsertArticle(article: Article) = savedArticleRepository.saveArticle(article)
    suspend fun deleteArticle(article: Article) = savedArticleRepository.deleteArticle(article)
}
