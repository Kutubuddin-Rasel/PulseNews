package com.example.newsapp.data.repository

import com.example.newsapp.Room.ArticleDao
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.example.newsapp.data.util.FirestoreSyncManager

class SavedArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val firestoreSyncManager: FirestoreSyncManager
) : SavedArticleRepository {
    override fun observeSavedArticles(): Flow<List<Article>> = articleDao.allArticle()

    override suspend fun saveArticle(article: Article) {
        articleDao.upsertArticle(article)
        firestoreSyncManager.pushArticleSave(article)
    }

    override suspend fun deleteArticle(article: Article) {
        articleDao.delete(article)
        firestoreSyncManager.pushArticleUnsave(article.url)
    }

    override suspend fun isSaved(url: String): Boolean = articleDao.isSaved(url)

    override suspend fun articleByUrl(url: String): Article? = articleDao.getByUrl(url)
}
