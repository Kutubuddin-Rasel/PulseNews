package com.example.newsapp.domain.usecase.saved

import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSavedArticlesUseCase @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) {
    operator fun invoke(): Flow<List<Article>> {
        return savedArticleRepository.observeSavedArticles()
    }
}
