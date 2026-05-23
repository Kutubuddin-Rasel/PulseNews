package com.example.newsapp.domain.util.tts

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class NativeTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private suspend fun initializeTts(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isInitialized && tts != null) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
                continuation.resume(true)
            } else {
                continuation.resume(false)
            }
        }
    }

    override suspend fun synthesizeToUri(text: String, articleId: String): Uri {
        val success = initializeTts()
        if (!success) {
            throw IllegalStateException("Failed to initialize Native TextToSpeech engine")
        }

        val cacheDir = File(context.cacheDir, "audio_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val outputFile = File(cacheDir, "article_$articleId.wav")
        
        // If the file already exists, just return it instantly (acts as a local cache)
        if (outputFile.exists() && outputFile.length() > 0) {
            return Uri.fromFile(outputFile)
        }

        return suspendCancellableCoroutine { continuation ->
            val utteranceId = "utterance_$articleId"

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    continuation.resume(Uri.fromFile(outputFile))
                }

                @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                override fun onError(utteranceId: String?) {
                    continuation.resumeWithException(RuntimeException("TTS Synthesis failed"))
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    continuation.resumeWithException(RuntimeException("TTS Synthesis failed with code $errorCode"))
                }
            })

            // Synthesize text to the local file
            val result = tts?.synthesizeToFile(text, null, outputFile, utteranceId)
            
            if (result == TextToSpeech.ERROR) {
                continuation.resumeWithException(RuntimeException("Failed to queue synthesis"))
            }

            continuation.invokeOnCancellation {
                tts?.stop()
            }
        }
    }
}
