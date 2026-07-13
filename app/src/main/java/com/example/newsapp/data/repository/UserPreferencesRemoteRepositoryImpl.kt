package com.example.newsapp.data.repository

import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.remote.dto.UpdatePreferencesRequest
import com.example.newsapp.domain.repository.UserPreferencesRemoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRemoteRepositoryImpl @Inject constructor(
    private val api: PulseBackendApi
) : UserPreferencesRemoteRepository {

    override suspend fun updatePreferences(regions: List<String>?, languages: List<String>?) {
        withContext(Dispatchers.IO) {
            api.updateUserPreferences(
                UpdatePreferencesRequest(
                    preferredRegions = regions,
                    preferredLanguages = languages
                )
            )
        }
    }
}
