package com.example.newsapp.domain.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineHtmlCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val cacheDir = File(context.cacheDir, "offline_articles").apply {
        if (!exists()) mkdirs()
    }

    suspend fun fetchAndCacheHtml(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val document = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()
            
            // We save the full outerHtml to preserve <meta> tags used by HtmlParser
            val cleanHtml = document.outerHtml()
            
            if (cleanHtml.isNotBlank()) {
                val file = getFileForUrl(url)
                file.writeText(cleanHtml)
                return@withContext true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCachedHtml(url: String): String? = withContext(Dispatchers.IO) {
        val file = getFileForUrl(url)
        if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    private fun getFileForUrl(url: String): File {
        val hash = hashUrl(url)
        return File(cacheDir, "$hash.html")
    }

    fun hasCachedHtml(url: String): Boolean {
        return getFileForUrl(url).exists()
    }

    private fun hashUrl(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(url.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
