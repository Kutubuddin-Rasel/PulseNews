package com.example.newsapp.domain.util.tts

import android.net.Uri

/**
 * Enterprise Text-to-Speech Engine Interface.
 * Allows seamless hot-swapping between the offline Native Android TextToSpeech 
 * and premium Cloud-based neural TTS providers.
 */
interface TtsEngine {
    /**
     * Synthesizes the given text into an audio file.
     * @param text The text of the article to narrate.
     * @param articleId A unique identifier used to name the cached audio file.
     * @return The local Uri of the generated audio file.
     */
    suspend fun synthesizeToUri(text: String, articleId: String): Uri
}
