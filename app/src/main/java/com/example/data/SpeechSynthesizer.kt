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
    private var pendingLanguageTag: String? = null

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
                applyLanguage(pendingLanguageTag ?: Locale.getDefault().toLanguageTag())
                tts?.setSpeechRate(1f)
            } else {
                initError = "The text to speech engine is not available on this device."
                AppLog.error("SpeechSynthesizer", initError!!)
            }
            onReady(ready)
        }
    }

    /**
     * Selects the synthesis language by BCP-47 tag ("bn-IN", "en-US", ...).
     * Call before or after [initialise]; when the engine has no voice for the
     * requested language the result says so instead of silently speaking
     * Bangla text with an English voice.
     *
     * @return a short status line; empty when the language is available.
     */
    fun setLanguage(languageTag: String): String {
        pendingLanguageTag = languageTag
        val engine = tts ?: return ""
        val locale = Locale.forLanguageTag(languageTag)
        val availability = engine.isLanguageAvailable(locale)
        return if (availability < TextToSpeech.LANG_AVAILABLE) {
            engine.language = Locale.getDefault()
            "No ${locale.displayLanguage} voice is installed on this device - " +
                "using the device default instead. Install a ${locale.displayLanguage} " +
                "text-to-speech voice in system settings for real ${locale.displayLanguage} speech."
        } else {
            engine.language = locale
            ""
        }
    }

    private fun applyLanguage(languageTag: String) {
        val message = setLanguage(languageTag)
        if (message.isNotEmpty()) AppLog.warn("SpeechSynthesizer", message)
    }

    /**
     * Synthesises [text] into a wave file and returns it. The synthesis runs in
     * the background, [onFinished] receives the generated file.
     */
    fun synthesize(
        text: String,
        languageTag: String? = null,
        onFinished: (File?, String?) -> Unit
    ) {
        if (!ready) {
            onFinished(null, initError ?: "The text to speech engine is still starting.")
            return
        }
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            onFinished(null, "Write something first.")
            return
        }
        val languageNote = languageTag?.let { setLanguage(it) } ?: ""
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
        } else if (languageNote.isNotEmpty()) {
            // Surface the voice availability warning through the same channel
            // the caller already shows errors on.
            onFinished(target, languageNote)
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
