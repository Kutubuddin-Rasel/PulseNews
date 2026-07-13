package com.example.newsapp.domain.usecase.news

import com.example.newsapp.domain.model.TrendingTopic
import com.example.newsapp.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrendingTopicsUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    operator fun invoke(): Flow<List<TrendingTopic>> {
        return newsRepository.getTrendingTopics()
    }
}
