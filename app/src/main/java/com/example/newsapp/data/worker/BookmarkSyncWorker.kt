package com.example.newsapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.remote.dto.BookmarkRequest
import com.example.newsapp.data.util.DeviceIdProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

@HiltWorker
class BookmarkSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pulseBackendApi: PulseBackendApi,
    private val deviceIdProvider: DeviceIdProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(KEY_ARTICLE_ID) ?: return Result.failure()
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()

        return try {
            val deviceId = deviceIdProvider.deviceId
            val response = if (action == ACTION_ADD) {
                pulseBackendApi.addBookmark(deviceId, BookmarkRequest(articleId))
            } else {
                pulseBackendApi.removeBookmark(deviceId, articleId)
            }

            if (response.isSuccessful) {
                Result.success()
            } else {
                // If it's a client error (e.g., 400 Bad Request, 404 Not Found), don't retry.
                // 404 could happen if trying to delete a bookmark that's already deleted on server.
                if (response.code() in 400..499) {
                    Result.failure()
                } else {
                    Result.retry() // Server error (5xx), try again later
                }
            }
        } catch (e: IOException) {
            // Network error
            Result.retry()
        } catch (e: HttpException) {
            // Unexpected HTTP exception
            if (e.code() in 400..499) Result.failure() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_ARTICLE_ID = "article_id"
        const val KEY_ACTION = "action"
        const val ACTION_ADD = "ADD"
        const val ACTION_DELETE = "DELETE"
    }
}
