package com.example.newsapp.domain.model

sealed interface AppError {
    data object Unauthorized : AppError
    data object RateLimited : AppError
    data object NoConnection : AppError
    data object Server : AppError
    data object Unknown : AppError
}
