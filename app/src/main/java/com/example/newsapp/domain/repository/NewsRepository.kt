package com.example.newsapp.domain.repository

import androidx.paging.PagingData
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.model.TrendingTopic
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow

import com.example.newsapp.data.remote.dto.PulseMetaDto

interface NewsRepository {
    fun getFeed(categoryKey: CategoryKey, source: String? = null): Flow<PagingData<Article>>
    fun searchNews(query: String, excludeId: String? = null): Flow<PagingData<Article>>
    fun getAvailableSources(categoryKey: CategoryKey): Flow<List<String>>
    suspend fun cachedArticleByUrl(url: String): Article?
    fun getTrendingTopics(): Flow<List<TrendingTopic>>
}
