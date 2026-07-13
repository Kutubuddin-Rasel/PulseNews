package com.example.newsapp.domain.repository

import com.example.newsapp.domain.model.CategoryKey
import kotlinx.coroutines.flow.Flow

interface TaxonomyRepository {
    val dictionaryFlow: Flow<Map<CategoryKey, List<String>>>
    suspend fun getVersion(): String
    suspend fun getLastFetchedTime(): Long
    suspend fun updateLastFetchedTime(timestamp: Long)
    suspend fun saveTaxonomy(version: String, categories: Map<String, List<String>>, timestamp: Long)
}
