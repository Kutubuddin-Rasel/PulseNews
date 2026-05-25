package com.example.newsapp.domain.util

interface KeywordMatcher {
    /**
     * Scans the provided text and returns a map of topics to their match frequencies.
     * @param text The full text to scan.
     * @return A map where the key is the topic name and the value is the number of keyword matches.
     */
    fun matchFrequencies(text: String): Map<String, Int>
}
