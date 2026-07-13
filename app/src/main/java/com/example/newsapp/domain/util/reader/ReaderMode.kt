package com.example.newsapp.domain.util.reader

/** Which surface renders the article: the cleaned in-app Reader, or the live in-app WebView. */
enum class ReaderMode { Reader, Web }

fun ReaderMode.toggled(): ReaderMode =
    if (this == ReaderMode.Reader) ReaderMode.Web else ReaderMode.Reader

/** Quality gate: thin extractions open straight into the WebView so the user never lands
 * on a near-empty Reader page; rich extractions open in Reader. */
fun initialModeFor(isThin: Boolean): ReaderMode =
    if (isThin) ReaderMode.Web else ReaderMode.Reader
