package com.example.newsapp.Repository

import com.example.newsapp.domain.model.UiState
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Deprecated("Use domain.repository.NewsRepository directly")
class TopHeadlineRepository @Inject constructor(
    private val newsRepository: NewsRepository
) {
    fun stream(category: String, page: Int = 1): Flow<UiState<List<Article>>> {
        return newsRepository.topHeadlines(category, page)
    }
}
