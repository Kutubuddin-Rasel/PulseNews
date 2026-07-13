package com.example.newsapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface RecentSearchRepository {
    fun getRecentSearches(): Flow<List<String>>
    suspend fun recordRecentSearch(query: String)
    suspend fun clearRecentSearches()
}
