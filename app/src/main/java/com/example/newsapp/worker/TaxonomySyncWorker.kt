package com.example.newsapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.repository.TaxonomyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TaxonomySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: PulseBackendApi,
    private val taxonomyRepository: TaxonomyRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val response = api.getTaxonomy()
            if (response.isSuccessful) {
                val taxonomyDto = response.body()
                if (taxonomyDto != null) {
                    val currentVersion = taxonomyRepository.getVersion()
                    
                    // Simple string comparison for version, or always overwrite if different
                    if (taxonomyDto.version != currentVersion) {
                        taxonomyRepository.saveTaxonomy(taxonomyDto.version, taxonomyDto.categories)
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
