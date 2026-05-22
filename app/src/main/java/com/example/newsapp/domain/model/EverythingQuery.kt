package com.example.newsapp.domain.model

data class EverythingQuery(
    val topic: String,
    val sortBy: String,
    val page: Int = 1,
    val pageSize: Int = 20
)
