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

import com.example.newsapp.domain.util.OfflineHtmlCache

@HiltWorker
class NewsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val newsRepository: NewsRepository,
    private val offlineHtmlCache: OfflineHtmlCache
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Fetch top headlines in background to prime the cache (triggers RemoteMediator)
            newsRepository.topHeadlines(category = "").firstOrNull()

            // Ideally we'd get the cached URLs from the DB here, but for simplicity we rely on the 
            // RemoteMediator having populated the cache. The UI will fetch HTML on demand, or 
            // we could inject the DAO to get the first 10 URLs.
            // Result.success() will be returned.
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
