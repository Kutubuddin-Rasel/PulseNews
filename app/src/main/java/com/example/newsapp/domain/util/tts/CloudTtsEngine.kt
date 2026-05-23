package com.example.newsapp.domain.util.tts

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudTtsEngine @Inject constructor(
    private val nativeFallback: NativeTtsEngine
) : TtsEngine {

    // In the future, this class will implement Google Cloud TTS or ElevenLabs via an API Key.
    // Since the user requested the interface to default to Native but support Cloud,
    // this skeleton acts as a pass-through to Native until the cloud endpoint is built.

    override suspend fun synthesizeToUri(text: String, articleId: String): Uri {
        // TODO: Implement cloud API call here. 
        // 1. Check if Cloud API Key is present in BuildConfig.
        // 2. If present, make Retrofit call to cloud TTS provider.
        // 3. Save streamed bytes to local File and return Uri.
        // 4. If network fails or key is missing, fallback to native.
        
        return nativeFallback.synthesizeToUri(text, articleId)
    }
}
