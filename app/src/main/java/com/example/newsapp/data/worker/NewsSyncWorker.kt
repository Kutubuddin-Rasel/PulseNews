package com.example.newsapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.repository.NewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

import com.example.newsapp.Room.CachedFeedDao
import com.example.newsapp.domain.util.OfflineHtmlCache

@HiltWorker
class NewsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val newsRepository: NewsRepository,
    private val cachedFeedDao: CachedFeedDao,
    private val offlineHtmlCache: OfflineHtmlCache
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Ultra-Fast REST Polling: Sync the firehose directly
            val syncResult = newsRepository.syncFirehose()
            
            if (syncResult.isSuccess) {
                // 2. Pre-fetch HTML for the top 5 trending articles for instant offline reading
                val topUrls = cachedFeedDao.getTopUrls(feedKey = "firehose", limit = 5)
                for (url in topUrls) {
                    if (!offlineHtmlCache.hasCachedHtml(url)) {
                        offlineHtmlCache.fetchAndCacheHtml(url)
                    }
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
