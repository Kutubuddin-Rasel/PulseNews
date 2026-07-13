package com.example.newsapp.data.util

import java.util.Locale

/** Curated region → local ISO-639-1 language(s). Multi-lingual regions list all. */
private val REGION_LANGUAGES: Map<String, List<String>> = mapOf(
    "DE" to listOf("de"), "AT" to listOf("de"), "CH" to listOf("de", "fr", "it"),
    "FR" to listOf("fr"), "BE" to listOf("nl", "fr"), "IT" to listOf("it"),
    "ES" to listOf("es"), "MX" to listOf("es"), "AR" to listOf("es"),
    "PT" to listOf("pt"), "BR" to listOf("pt"), "NL" to listOf("nl"),
    "SE" to listOf("sv"), "DK" to listOf("da"), "FI" to listOf("fi"),
    "PL" to listOf("pl"), "CZ" to listOf("cs"), "HU" to listOf("hu"),
    "RO" to listOf("ro"), "GR" to listOf("el"), "TR" to listOf("tr"),
    "RU" to listOf("ru"), "UA" to listOf("uk"), "JP" to listOf("ja"),
    "KR" to listOf("ko"), "CN" to listOf("zh"), "TW" to listOf("zh"),
    "VN" to listOf("vi"), "TH" to listOf("th"), "ID" to listOf("id"),
    "IL" to listOf("he"), "SA" to listOf("ar"), "AE" to listOf("ar"),
    "EG" to listOf("ar"), "IN" to listOf("hi", "en"),
)

/** What languages this region's local audience reads. Empty for unknown regions. */
fun regionLanguages(region: String?): List<String> =
    region?.uppercase(Locale.ROOT)?.let { REGION_LANGUAGES[it] } ?: emptyList()

/**
 * The user's readable language set: device language ∪ English ∪ the region's
 * local language(s), de-duplicated, in priority order. Fixes the
 * "everyone's phone is English" case by injecting the region's local language,
 * so a US-English phone in Germany still surfaces German articles.
 */
fun resolveReadableLanguages(region: String?, deviceLang: String): List<String> {
    val dev = deviceLang.lowercase(Locale.ROOT)
    return (listOf(dev, "en") + regionLanguages(region)).distinct()
}
