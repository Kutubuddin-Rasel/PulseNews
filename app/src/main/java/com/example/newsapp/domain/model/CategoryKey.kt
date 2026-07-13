package com.example.newsapp.domain.model

@JvmInline
value class CategoryKey(val value: String) {
    companion object {
        // `value` MUST match a canonical label emitted by the worker classifier
        // (Rust/src/ai/categories.rs) and seeded into the backend taxonomy. The
        // live chips are built dynamically from the fetched dictionary, so these
        // are reference constants — but they must stay canonical so any code that
        // does reach for them queries a label that actually exists on articles
        // (the old "technology"/"finance" values returned an empty feed).
        val FOR_YOU = CategoryKey("for_you")
        val TECH = CategoryKey("tech")
        val BUSINESS = CategoryKey("business")
        val SPORTS = CategoryKey("sports")
        val POLITICS = CategoryKey("politics")
        val HEALTH = CategoryKey("health")
        val SCIENCE = CategoryKey("science")
        val ENTERTAINMENT = CategoryKey("entertainment")
        val WORLD = CategoryKey("world")
        val CRYPTO = CategoryKey("crypto")
        val DESIGN = CategoryKey("design")
    }
}
