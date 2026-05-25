package com.example.newsapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.domain.repository.NewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.example.newsapp.data.repository.NewsRepositoryImpl

@HiltWorker
class ScoreRecalculationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val newsRepository: NewsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Check if it's the specific implementation since the interface might not have this method
            if (newsRepository is NewsRepositoryImpl) {
                newsRepository.recalculateAllScores()
            }
            Result.success()
        } catch (e: Exception) {
            // If the recalculation crashes (e.g. database locked), we can retry it safely
            // because the chunking algorithm keeps track of where it is and uses REPLACE/UPDATE
            Result.retry()
        }
    }
}
