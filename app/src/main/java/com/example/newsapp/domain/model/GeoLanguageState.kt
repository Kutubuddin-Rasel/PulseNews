package com.example.newsapp.domain.model

/**
 * The resolved geo/language signal the app sends with feed requests and (for
 * signed-in users) persists to the backend. The two override flags record
 * whether the user manually set the value, so auto-detection on launch never
 * clobbers an explicit choice.
 */
data class GeoLanguageState(
    val homeRegion: String? = null,
    val readableLanguages: List<String> = emptyList(),
    val regionIsManualOverride: Boolean = false,
    val languagesIsManualOverride: Boolean = false,
)
