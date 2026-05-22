package com.example.newsapp.module

data class News(
    val articles: List<Article>,
    val status: String,
    val totalResults: Int
)
