package com.example.newsapp

import androidx.navigation.NavController
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun encodeNavUrl(url: String): String {
    return URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
}

fun decodeNavUrl(value: String): String {
    return URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
}

fun NavController.navigateToArticleDetail(url: String) {
    val encodedUrl = encodeNavUrl(url)
    this.navigate("${Routes.articleDetail}/$encodedUrl")
}

fun NavController.navigateToWebPage(url: String) {
    val encodedUrl = encodeNavUrl(url)
    this.navigate("${Routes.webPage}/$encodedUrl")
}
