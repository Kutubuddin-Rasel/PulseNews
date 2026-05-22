package com.example.newsapp

object Routes {
    const val home = "home"
    const val saved = "saved"
    const val settings = "settings"
    const val notificationPreferences = "notificationPreferences"
    const val algorithmPreferences = "algorithmPreferences"

    const val articleUrlArg = "url"

    const val articleDetail = "articleDetail"
    const val webPage = "webpage"

    const val articleDetailPattern = "$articleDetail/{$articleUrlArg}"
    const val webPagePattern = "$webPage/{$articleUrlArg}"
}
