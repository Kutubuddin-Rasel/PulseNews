package com.example.newsapp.domain.util

interface ClockProvider {
    fun nowMillis(): Long
}
