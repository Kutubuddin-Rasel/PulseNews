package com.example.newsapp.domain.usecase.saved

import com.example.newsapp.domain.repository.SavedArticleRepository
import javax.inject.Inject

class SyncBookmarksUseCase @Inject constructor(
    private val savedArticleRepository: SavedArticleRepository
) {
    suspend operator fun invoke() {
        savedArticleRepository.syncBookmarks()
    }
}
