package com.example.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Neural AI Voice Dubbing & Text-to-Speech Engine.
 */
class AiVoiceEngine(context: Context, onInitialized: (Boolean) -> Unit) {

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.Builder().setLanguage("bn").setRegion("BD").build()
                onInitialized(true)
            } else {
                onInitialized(false)
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AiVoiceDub")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
