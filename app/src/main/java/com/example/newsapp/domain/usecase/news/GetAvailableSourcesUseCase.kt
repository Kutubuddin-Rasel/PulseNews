package com.example.newsapp.domain.usecase.news

import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAvailableSourcesUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(categoryKey: CategoryKey): Flow<List<String>> {
        return newsRepository.getAvailableSources(categoryKey)
    }
}
