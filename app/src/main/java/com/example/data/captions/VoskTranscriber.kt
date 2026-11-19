package com.example.data.captions

import com.example.data.AppLog
import com.example.engine.SpeechTranscriber
import com.example.engine.WordTiming
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Real on-device speech recognition through a Vosk model ([TranscriptionModels]
 * owns the download). Everything runs offline: decoded PCM goes in, timed
 * words come out, [com.example.engine.CaptionAssembler] turns them into cues.
 */
class VoskTranscriber(private val modelPath: String) : SpeechTranscriber {

    override fun recognize(samples: ShortArray, sampleRate: Int): List<WordTiming> {
        if (samples.isEmpty()) return emptyList()
        val model = Model(modelPath)
        try {
            val recognizer = Recognizer(model, sampleRate.toFloat())
            try {
                recognizer.setWords(true)
                val pcm = toLittleEndianBytes(samples)
                val chunkBytes = sampleRate * BYTES_PER_SAMPLE * CHUNK_SECONDS
                var offset = 0
                while (offset < pcm.size) {
                    val len = minOf(chunkBytes, pcm.size - offset)
                    val chunk = ByteArray(len)
                    System.arraycopy(pcm, offset, chunk, 0, len)
                    recognizer.acceptWaveForm(chunk, len)
                    offset += len
                }
                val words = wordsFromResult(recognizer.finalResult).toMutableList()
                if (words.isEmpty()) {
                    // Some models only emit word timings on intermediate results.
                    words += wordsFromResult(recognizer.result)
                }
                return words
            } finally {
                recognizer.close()
            }
        } catch (error: Throwable) {
            AppLog.error(TAG, "Vosk recognition failed", error)
            throw error
        } finally {
            model.close()
        }
    }

    companion object {
        private const val TAG = "VoskTranscriber"
        private const val BYTES_PER_SAMPLE = 2
        private const val CHUNK_SECONDS = 1

        /** Interleaved 16-bit PCM as the little-endian bytes Vosk expects. */
        fun toLittleEndianBytes(samples: ShortArray): ByteArray {
            val buffer = ByteBuffer.allocate(samples.size * BYTES_PER_SAMPLE)
                .order(ByteOrder.LITTLE_ENDIAN)
            buffer.asShortBuffer().put(samples)
            return buffer.array()
        }

        /**
         * Parses a Vosk result JSON into word timings. Times come back in
         * seconds; milliseconds are absolute from the start of the audio.
         */
        fun wordsFromResult(json: String): List<WordTiming> {
            if (json.isBlank()) return emptyList()
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
            val words: JSONArray = root.optJSONArray("result") ?: return emptyList()
            return (0 until words.length()).mapNotNull { index ->
                val word = words.optJSONObject(index) ?: return@mapNotNull null
                val text = word.optString("word").trim()
                val startSeconds = word.optDouble("start", Double.NaN)
                val endSeconds = word.optDouble("end", Double.NaN)
                if (text.isBlank() || !startSeconds.isFinite() || !endSeconds.isFinite()) {
                    return@mapNotNull null
                }
                val start = (startSeconds * 1000).toLong().coerceAtLeast(0L)
                WordTiming(text, start, (endSeconds * 1000).toLong().coerceAtLeast(start))
            }
        }
    }
}
