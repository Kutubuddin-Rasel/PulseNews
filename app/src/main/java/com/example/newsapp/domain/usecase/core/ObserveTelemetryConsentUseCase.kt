package com.example.newsapp.domain.usecase.core

import com.example.newsapp.domain.repository.PrivacyPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTelemetryConsentUseCase @Inject constructor(
    private val privacyPreferencesRepository: PrivacyPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean?> {
        return privacyPreferencesRepository.telemetryConsent
    }
}
