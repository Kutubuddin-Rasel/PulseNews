package com.example.newsapp.Repository

import com.example.newsapp.domain.model.EverythingQuery
import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Deprecated("Use domain.repository.NewsRepository directly")
class EveryNewsRepository @Inject constructor(
    private val newsRepository: NewsRepository
) {
    fun stream(query: EverythingQuery): Flow<UiState<List<Article>>> {
        return newsRepository.everything(query)
    }
}
