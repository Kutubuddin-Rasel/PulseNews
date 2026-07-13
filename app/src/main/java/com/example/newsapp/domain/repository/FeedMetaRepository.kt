package com.example.newsapp.domain.repository

import com.example.newsapp.data.remote.dto.PulseMetaDto
import kotlinx.coroutines.flow.Flow

interface FeedMetaRepository {
    val meta: Flow<PulseMetaDto?>

    /** O7: epoch-millis of the last successful meta fetch, or null if never fetched. Drives the
     *  meta-refresh TTL so the meta endpoint isn't re-hit on every feed refresh. */
    val metaFetchedAtMs: Flow<Long?>

    suspend fun saveMeta(totalPages: Int, lastUpdated: String, fetchedAtMs: Long)
}
