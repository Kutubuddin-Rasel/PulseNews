package com.example.newsapp.domain.usecase.news

import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.module.Article
import javax.inject.Inject

class GetCachedArticleUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(url: String): Article? {
        return newsRepository.cachedArticleByUrl(url)
    }
}
