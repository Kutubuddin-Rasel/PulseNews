package com.example.newsapp.domain.repository

/**
 * Durable, server-side persistence of a signed-in user's geo/language
 * preferences (PUT api/user/preferences). Separate from the local
 * [GeoLanguageRepository] (DataStore) so the local fast-path stays offline-only
 * and this remote call can be best-effort.
 */
interface UserPreferencesRemoteRepository {
    /**
     * Push preferences to the backend. A null field is omitted server-side and
     * leaves the stored value untouched (Wave 1 sends only [regions]).
     */
    suspend fun updatePreferences(regions: List<String>? = null, languages: List<String>? = null)
}
