package com.example.newsapp.ui.components.reader

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** In-app WebView fallback for the Reader. Used when extraction is thin/empty or the user
 * toggles to the live page. Conservative settings (no file/content access) and a hard
 * destroy() on dispose to avoid the WebView leak class flagged in the prior audit. */
@Composable
fun ReaderWebView(url: String, onError: () -> Unit, modifier: Modifier = Modifier) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (request?.isForMainFrame == true) onError()
                    }
                }
                settings.javaScriptEnabled = true       // many article bodies need JS
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = true
                loadUrl(url)
                webView = this
            }
        },
        update = { if (it.url != url) it.loadUrl(url) }
    )
    DisposableEffect(Unit) { onDispose { webView?.destroy() } }
}
