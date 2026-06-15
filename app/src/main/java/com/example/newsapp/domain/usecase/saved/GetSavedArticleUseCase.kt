package com.example.newsapp.domain.usecase.saved

import com.example.newsapp.domain.repository.SavedArticleRepository
import com.example.newsapp.module.Article
import javax.inject.Inject

class GetSavedArticleUseCase @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) {
    suspend operator fun invoke(url: String): Article? {
        return savedArticleRepository.articleByUrl(url)
    }
}
