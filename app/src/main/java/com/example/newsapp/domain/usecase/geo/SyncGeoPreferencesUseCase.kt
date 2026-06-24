package com.example.newsapp.domain.usecase.geo

import com.example.newsapp.data.util.RegionDetector
import com.example.newsapp.data.util.resolveReadableLanguages
import com.example.newsapp.domain.repository.AuthRepository
import com.example.newsapp.domain.repository.GeoLanguageRepository
import com.example.newsapp.domain.repository.UserPreferencesRemoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

/**
 * On launch: detect the device region + readable languages, apply them locally
 * (never clobbering a manual override — see [GeoLanguageRepository.applyDetected]),
 * and, for a signed-in user, best-effort persist both to the backend so the
 * For-You ranker has the geo/language signal even on a cold device.
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
        // Apply the detected region first (respecting a manual override) so the
        // language resolution below keys off the EFFECTIVE region — a user who
        // pinned region=JP on a US-locale phone still gets Japanese.
        geoLanguageRepository.applyDetected(region = detected, languages = emptyList())

        // Wave 2: derive the readable language set from the effective region + the
        // device language, then apply it. region=null leaves the region untouched on
        // this second call; applyDetected still skips languages on a manual override.
        val effectiveRegion = geoLanguageRepository.state.first().homeRegion
        val deviceLang = Locale.getDefault().language
        val languages = resolveReadableLanguages(effectiveRegion, deviceLang)
        geoLanguageRepository.applyDetected(region = null, languages = languages)

        val resolved = geoLanguageRepository.state.first()
        val region = resolved.homeRegion
        val langs = resolved.readableLanguages
        if (authRepository.currentUser.value != null && (region != null || langs.isNotEmpty())) {
            runCatching {
                remote.updatePreferences(
                    regions = region?.let { listOf(it) },
                    languages = langs.ifEmpty { null },
                )
            }
        }
    }
}
