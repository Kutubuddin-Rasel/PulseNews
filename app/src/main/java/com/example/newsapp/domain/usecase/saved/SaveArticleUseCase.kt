package com.example.newsapp.domain.usecase.saved

import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import javax.inject.Inject

class SaveArticleUseCase @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) {
    suspend operator fun invoke(article: Article) {
        savedArticleRepository.saveArticle(article)
    }
}
