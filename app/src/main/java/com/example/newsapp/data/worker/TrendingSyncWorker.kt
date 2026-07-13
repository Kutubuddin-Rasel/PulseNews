package com.example.newsapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.Room.TrendingTopicDao
import com.example.newsapp.Room.TrendingTopicEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TrendingSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: PulseBackendApi,
    private val trendingTopicDao: TrendingTopicDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val response = api.getTrendingTopics()
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val entities = dtos.map {
                    TrendingTopicEntity(
                        tag = it.tag,
                        count = it.count,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                
                // Replace old trending data with fresh data
                trendingTopicDao.clearTopics()
                trendingTopicDao.insertTopics(entities)
                
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
