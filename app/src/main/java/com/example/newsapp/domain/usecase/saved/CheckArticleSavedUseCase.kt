package com.example.newsapp.domain.usecase.saved

import com.example.newsapp.domain.repository.SavedArticleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckArticleSavedUseCase @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) {
    suspend operator fun invoke(url: String): Boolean {
        return savedArticleRepository.isSaved(url)
    }
}
