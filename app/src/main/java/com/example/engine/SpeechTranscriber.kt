package com.example.engine

/**
 * The offline speech-to-text contract behind real auto captions.
 *
 * Implementations receive decoded mono PCM and return per-word timings. The
 * production implementation ([com.example.data.captions.VoskTranscriber])
 * wraps a downloaded Vosk model; tests inject a fake, so the whole caption
 * pipeline is verifiable without any model on disk.
 */
interface SpeechTranscriber {

    /**
     * Transcribes interleaved 16-bit PCM. The default expectation is 16 kHz
     * mono; implementations that need another rate say so via [sampleRate].
     *
     * @return words with absolute millisecond timings (relative to the start
     *   of the PCM), in speaking order. Empty when nothing was recognised.
     */
    fun recognize(samples: ShortArray, sampleRate: Int): List<WordTiming>
}

/** One recognised word. Timings are absolute milliseconds into the PCM. */
data class WordTiming(
    val text: String,
    val startMs: Long,
    val endMs: Long
) {
    val isSane: Boolean
        get() = text.isNotBlank() && endMs > startMs
}
