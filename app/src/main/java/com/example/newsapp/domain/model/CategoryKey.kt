package com.example.newsapp.domain.model

@JvmInline
value class CategoryKey(val value: String) {
    companion object {
        val FOR_YOU = CategoryKey("for_you")
        val TECHNOLOGY = CategoryKey("technology")
        val BUSINESS = CategoryKey("business")
        val POLITICS = CategoryKey("politics")
        val SPORTS = CategoryKey("sports")
        val ENTERTAINMENT = CategoryKey("entertainment")
        val HEALTH = CategoryKey("health")
        val SCIENCE = CategoryKey("science")
        val WORLD = CategoryKey("world")
        val FINANCE = CategoryKey("finance")
    }
}
