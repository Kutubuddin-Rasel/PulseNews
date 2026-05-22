package com.example.newsapp.domain.model

sealed interface UiEvent {
    data class Saved(val message: String = "Article saved") : UiEvent
    data class AlreadySaved(val message: String = "Already saved") : UiEvent
    data class DeleteFailed(val message: String = "Unable to delete article") : UiEvent
    data class NetworkError(val message: String = "Network unavailable") : UiEvent
    data class Generic(val message: String) : UiEvent
}
