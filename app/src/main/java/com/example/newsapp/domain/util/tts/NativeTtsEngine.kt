package com.example.newsapp.domain.util.tts

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class NativeTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine {

    private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false

    // TTS3: a single global progress listener on the shared engine lets a second concurrent synth
    // overwrite the first's listener, so the first coroutine never resumes (a suspended-coroutine
    // leak). Instead we register ONE dispatching listener and route each callback to the right
    // pending call by utteranceId.
    private val pending = ConcurrentHashMap<String, PendingSynthesis>()

    private data class PendingSynthesis(
        val continuation: CancellableContinuation<Uri>,
        val outputFile: File
    )

    private val dispatchingListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            val p = utteranceId?.let { pending.remove(it) } ?: return
            if (p.continuation.isActive) p.continuation.resume(Uri.fromFile(p.outputFile))
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            val p = utteranceId?.let { pending.remove(it) } ?: return
            if (p.continuation.isActive) {
                p.continuation.resumeWithException(RuntimeException("TTS Synthesis failed"))
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val p = utteranceId?.let { pending.remove(it) } ?: return
            if (p.continuation.isActive) {
                p.continuation.resumeWithException(RuntimeException("TTS Synthesis failed with code $errorCode"))
            }
        }
    }

    private suspend fun initializeTts(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isInitialized && tts != null) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                // Register the single dispatching listener once, not per call (TTS3).
                tts?.setOnUtteranceProgressListener(dispatchingListener)
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

        // TTS2: bound audio_cache before writing a new clip.
        pruneCache(cacheDir)

        return suspendCancellableCoroutine { continuation ->
            val utteranceId = "utterance_$articleId"
            pending[utteranceId] = PendingSynthesis(continuation, outputFile)

            // Synthesize text to the local file
            val result = tts?.synthesizeToFile(text, null, outputFile, utteranceId)

            if (result == TextToSpeech.ERROR) {
                pending.remove(utteranceId)
                continuation.resumeWithException(RuntimeException("Failed to queue synthesis"))
            }

            continuation.invokeOnCancellation {
                pending.remove(utteranceId)
                tts?.stop()
            }
        }
    }

    /** TTS1: release the held system TextToSpeech binding; re-initializes lazily on next use. */
    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        pending.clear()
    }

    /** TTS2: keep audio_cache under a size cap, evicting oldest clips first (LRU by mtime). */
    private fun pruneCache(cacheDir: File) {
        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_AUDIO_CACHE_BYTES) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= MAX_AUDIO_CACHE_BYTES) break
            val len = file.length()
            if (file.delete()) total -= len
        }
    }

    companion object {
        private const val MAX_AUDIO_CACHE_BYTES = 50L * 1024 * 1024 // 50 MB
    }
}
