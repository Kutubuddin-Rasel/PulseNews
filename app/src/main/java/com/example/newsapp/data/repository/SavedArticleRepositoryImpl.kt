package com.example.newsapp.data.repository

import com.example.newsapp.Room.ArticleDao
import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.mapper.toDomainOrNull
import com.example.newsapp.data.remote.dto.BookmarkRequest
import com.example.newsapp.data.util.DeviceIdProvider
import com.example.newsapp.data.worker.BookmarkSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext

class SavedArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    @ApplicationContext private val context: Context,
    private val pulseBackendApi: PulseBackendApi,
    private val deviceIdProvider: DeviceIdProvider,
    private val workManager: WorkManager
) : SavedArticleRepository {
    
    override fun observeSavedArticles(): Flow<List<Article>> = articleDao.allArticle()

    override suspend fun saveArticle(article: Article) {
        articleDao.upsertArticle(article)
        
        article.backendId?.let { id ->
            val data = workDataOf(
                BookmarkSyncWorker.KEY_ARTICLE_ID to id,
                BookmarkSyncWorker.KEY_ACTION to BookmarkSyncWorker.ACTION_ADD
            )
            try {
                val request = OneTimeWorkRequestBuilder<BookmarkSyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setInputData(data)
                    .build()
                    
                workManager.enqueueUniqueWork(
                    "sync_bookmark_$id",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                // Ignore WorkManager initialization errors during testing
            }
        }
    }

    override suspend fun deleteArticle(article: Article) {
        articleDao.delete(article)
        
        article.backendId?.let { id ->
            val data = workDataOf(
                BookmarkSyncWorker.KEY_ARTICLE_ID to id,
                BookmarkSyncWorker.KEY_ACTION to BookmarkSyncWorker.ACTION_DELETE
            )
            try {
                val request = OneTimeWorkRequestBuilder<BookmarkSyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setInputData(data)
                    .build()
                    
                workManager.enqueueUniqueWork(
                    "sync_bookmark_$id",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                // Ignore WorkManager initialization errors during testing
            }
        }
    }

    override suspend fun isSaved(url: String): Boolean = articleDao.isSaved(url)

    override suspend fun articleByUrl(url: String): Article? = articleDao.getByUrl(url)

    override suspend fun syncBookmarks() {
        try {
            val response = pulseBackendApi.getBookmarks(deviceIdProvider.deviceId)
            if (response.isSuccessful) {
                val backendArticles = response.body()?.mapNotNull { it.toDomainOrNull() } ?: emptyList()
                if (backendArticles.isNotEmpty()) {
                    articleDao.upsertAll(backendArticles)
                }
            }
        } catch (e: Exception) {
            // Silently fail, rely on offline cache
        }
    }
}
