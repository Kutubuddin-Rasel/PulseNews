package com.example.newsapp.domain.usecase.news

import com.example.newsapp.data.remote.dto.PulseMetaDto
import com.example.newsapp.domain.repository.FeedMetaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFeedMetaUseCase @Inject constructor(
    private val feedMetaRepository: FeedMetaRepository
) {
    operator fun invoke(): Flow<PulseMetaDto?> {
        return feedMetaRepository.meta
    }
}
