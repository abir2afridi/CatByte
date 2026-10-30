package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Turns a piece of text into a real voice-over file using the Android text to
 * speech engine. The generated file can be added to the timeline like any other
 * audio track.
 */
class SpeechSynthesizer(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var initError: String? = null

    val isReady: Boolean get() = ready

    /** Initialises the engine. Returns true when the engine is usable. */
    fun initialise(onReady: (Boolean) -> Unit) {
        if (tts != null) {
            onReady(ready)
            return
        }
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(1f)
            } else {
                initError = "The text to speech engine is not available on this device."
                AppLog.error("SpeechSynthesizer", initError!!)
            }
            onReady(ready)
        }
    }

    /**
     * Synthesises [text] into a wave file and returns it. The synthesis runs in
     * the background, [onFinished] receives the generated file.
     */
    fun synthesize(text: String, onFinished: (File?, String?) -> Unit) {
        if (!ready) {
            onFinished(null, initError ?: "The text to speech engine is still starting.")
            return
        }
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            onFinished(null, "Write something first.")
            return
        }
        val directory = File(context.filesDir, "voiceovers").apply { mkdirs() }
        val target = File(directory, "tts_${UUID.randomUUID().toString().take(8)}.wav")

        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                if (target.exists() && target.length() > 0L) {
                    onFinished(target, null)
                } else {
                    onFinished(null, "The voice-over could not be generated.")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onFinished(null, "The voice-over could not be generated.")
            }
        }
        tts?.setOnUtteranceProgressListener(listener)

        val utteranceId = "catbyte-${System.currentTimeMillis()}"
        val result = runCatching {
            tts?.synthesizeToFile(cleanText, null, target, utteranceId)
        }.getOrElse {
            onFinished(null, it.message ?: "The voice-over could not be generated.")
            return
        }
        if (result != TextToSpeech.SUCCESS) {
            onFinished(null, "The voice-over could not be generated.")
        }
    }

    /** Stops any running synthesis and releases the engine. */
    fun release() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}

/**
 * Small helper that reads the duration of a media file with [MediaPlayer].
 * Used as a fallback when the metadata retriever cannot read the file.
 */
object MediaDurationFallback {

    fun durationMs(context: Context, uri: android.net.Uri): Long = runCatching {
        val player = MediaPlayer()
        try {
            player.setDataSource(context, uri)
            player.prepare()
            player.duration.toLong()
        } finally {
            runCatching { player.release() }
        }
    }.getOrDefault(0L)
}
