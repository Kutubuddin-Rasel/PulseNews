package com.example.newsapp.domain.usecase.news

import androidx.paging.PagingData
import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(categoryKey: CategoryKey, source: String? = null, forceRefresh: Boolean = false): Flow<PagingData<Article>> {
        return newsRepository.getFeed(categoryKey, source, forceRefresh)
    }
}
