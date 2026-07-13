package com.example.newsapp.domain.usecase.news

import androidx.paging.PagingData
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNewsUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(query: String): Flow<PagingData<Article>> {
        return newsRepository.searchNews(query)
    }
}
