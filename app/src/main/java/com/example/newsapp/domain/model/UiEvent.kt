package com.example.newsapp.domain.model

sealed interface UiEvent {
    val message: String
    data class Saved(override val message: String = "Article saved") : UiEvent
    data class AlreadySaved(override val message: String = "Already saved") : UiEvent
    data class DeleteFailed(override val message: String = "Unable to delete article") : UiEvent
    data class NetworkError(override val message: String = "Network unavailable") : UiEvent
    data class Generic(override val message: String) : UiEvent
}
