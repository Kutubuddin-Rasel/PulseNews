package com.example.newsapp.domain.model

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(
        val data: T,
        val fromCache: Boolean = false,
        val isStale: Boolean = false
    ) : UiState<T>

    data class Empty(
        val message: String,
        val fromCache: Boolean = false,
        val isStale: Boolean = false
    ) : UiState<Nothing>

    data class Error(
        val message: String,
        val retryable: Boolean,
        val type: AppError = AppError.Unknown
    ) : UiState<Nothing>
}
