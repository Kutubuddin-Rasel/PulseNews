package com.example.newsapp.domain.usecase.core

import com.example.newsapp.domain.repository.PrivacyPreferencesRepository
import javax.inject.Inject

class SetTelemetryConsentUseCase @Inject constructor(
    private val privacyPreferencesRepository: PrivacyPreferencesRepository
) {
    suspend operator fun invoke(granted: Boolean) {
        privacyPreferencesRepository.setConsent(granted)
    }
}
