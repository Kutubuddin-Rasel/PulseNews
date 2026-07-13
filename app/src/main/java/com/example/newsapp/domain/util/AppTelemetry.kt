package com.example.newsapp.domain.util

interface AppTelemetry {
    fun requestId(): String
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    
    fun trackClick(articleId: String)
    fun trackReadDeep(articleId: String, durationSeconds: Long, scrollDepthPercent: Int)
    fun trackBookmark(articleId: String)
    fun trackShare(articleId: String, platform: String)
    fun trackBlockSource(articleId: String, sourceDomain: String)
}
