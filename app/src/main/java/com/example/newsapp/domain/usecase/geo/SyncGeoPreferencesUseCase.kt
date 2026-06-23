package com.example.newsapp.domain.usecase.geo

import com.example.newsapp.data.util.RegionDetector
import com.example.newsapp.domain.repository.AuthRepository
import com.example.newsapp.domain.repository.GeoLanguageRepository
import com.example.newsapp.domain.repository.UserPreferencesRemoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * On launch: detect the device region, apply it locally (never clobbering a
 * manual override — see [GeoLanguageRepository.applyDetected]), and, for a
 * signed-in user, best-effort persist it to the backend so the For-You ranker
 * has the home-region signal even on a cold device.
 *
 * The remote persist is wrapped in [runCatching] — a network failure must never
 * block the feed from loading.
 */
class SyncGeoPreferencesUseCase @Inject constructor(
    private val regionDetector: RegionDetector,
    private val geoLanguageRepository: GeoLanguageRepository,
    private val authRepository: AuthRepository,
    private val remote: UserPreferencesRemoteRepository
) {
    suspend operator fun invoke() {
        val detected = withContext(Dispatchers.IO) { regionDetector.detectRegion() }
        // Wave 1: only the region is auto-detected. Preserve whatever languages are
        // already stored so applyDetected doesn't blank them.
        val storedLanguages = geoLanguageRepository.state.first().readableLanguages
        geoLanguageRepository.applyDetected(region = detected, languages = storedLanguages)

        val resolved = geoLanguageRepository.state.first()
        val region = resolved.homeRegion
        if (authRepository.currentUser.value != null && region != null) {
            runCatching {
                remote.updatePreferences(regions = listOf(region))
            }
        }
    }
}
