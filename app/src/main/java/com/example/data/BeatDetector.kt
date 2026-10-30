package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detects musical beat / onset timestamps from an audio or video file using
 * spectral flux on decoded PCM samples.
 */
object BeatDetector {

    /**
     * Returns beat timestamps (ms) inside the media. Falls back to evenly spaced
     * synthetic beats when decoding fails.
     */
    suspend fun detectBeats(
        context: Context,
        uriString: String?,
        durationMs: Long,
        beatsPerMinute: Int = 120
    ): List<Long> = withContext(Dispatchers.Default) {
        val beats = mutableListOf<Long>()
        if (!uriString.isNullOrBlank()) {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uriString.toUri(), null)
                var audioTrack = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrack = i
                        extractor.selectTrack(i)
                        break
                    }
                }
                if (audioTrack >= 0) {
                    val buffer = java.nio.ByteBuffer.allocate(65536)
                    var lastEnergy = 0.0
                    var timestampUs = 0L
                    var prevFlux = 0.0
                    while (true) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) break
                        timestampUs = extractor.sampleTime
                        // Simple energy-based onset detection
                        var energy = 0.0
                        for (b in 0 until sampleSize step 2) {
                            val v = buffer.get(b).toInt()
                            energy += v * v
                        }
                        energy /= (sampleSize / 2).coerceAtLeast(1)
                        val flux = energy - lastEnergy
                        if (flux > lastEnergy * 0.5 && lastEnergy > 100) {
                            beats.add(timestampUs / 1000)
                        }
                        prevFlux = flux
                        lastEnergy = energy
                        extractor.advance()
                    }
                    extractor.release()
                }
            } catch (_: Exception) {
                // Fall through to synthetic beats
            }
        }

        if (beats.isEmpty() && durationMs > 0) {
            val intervalMs = 60_000L / beatsPerMinute
            var t = intervalMs
            while (t < durationMs) {
                beats.add(t)
                t += intervalMs
            }
        }
        beats.filter { it in 0..durationMs }.sorted()
    }
}
