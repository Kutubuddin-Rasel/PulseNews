package com.example.newsapp.domain.repository

import androidx.paging.PagingData
import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun topHeadlines(category: String): Flow<PagingData<Article>>
    fun everything(query: EverythingQuery): Flow<PagingData<Article>>
    suspend fun cachedArticleByUrl(url: String): Article?
}
