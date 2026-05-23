package com.example.newsapp.domain.repository

import androidx.paging.PagingData
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getFeed(categoryId: Int, keyword: String? = null, source: String? = null): Flow<PagingData<Article>>
    fun getAvailableSources(): Flow<List<String>>
    suspend fun cachedArticleByUrl(url: String): Article?
    suspend fun syncFirehose(): Result<Unit>
    suspend fun getNewsMetaLastUpdated(): Result<String?>
}
