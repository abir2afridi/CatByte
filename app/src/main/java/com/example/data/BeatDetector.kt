package com.example.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.core.net.toUri
import com.example.engine.BeatGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

/**
 * Detects musical beats from the real decoded PCM of an audio or video file.
 *
 * The audio track is decoded with MediaCodec, reduced to an RMS envelope (one
 * value per [HOP_MS] window) and converted to onset strengths by
 * half-wave-rectified flux. [BeatGrid] then estimates the tempo and lays the
 * grid. Nothing here is synthesised: when the file cannot be decoded, or the
 * audio has no usable onsets, the result is empty and the UI reports that
 * instead of drawing invented beats.
 */
object BeatDetector {

    private const val TAG = "BeatDetector"

    /** Duration of one envelope window. 20 ms is short enough to place beats. */
    const val HOP_MS = 20L

    /**
     * Beat analysis stops after five minutes. Longer sources rarely need
     * magnets past this point and a bounded pass keeps project switches cheap.
     */
    private const val MAX_ANALYSIS_MS = 300_000L

    private const val READ_TIMEOUT_US = 10_000L

    /**
     * Returns beat timestamps (ms, relative to the source file) for [uriString].
     * Empty means "no beats could be measured" - never a synthetic grid.
     */
    suspend fun detectBeats(context: Context, uriString: String?): List<Long> =
        withContext(Dispatchers.Default) {
            if (uriString.isNullOrBlank()) return@withContext emptyList()
            val envelope = runCatching { energyEnvelope(context, uriString.toUri()) }
                .onFailure { AppLog.warn(TAG, "Beat analysis failed for $uriString", it) }
                .getOrNull()
                ?: return@withContext emptyList()
            beatsFromEnvelope(envelope, HOP_MS)
        }

    /**
     * Converts the RMS envelope into beats.
     *
     * Onsets come from the positive rise between windows, so a quiet passage
     * that opens into a hit registers even when its absolute level is low.
     * A stable tempo becomes a regular grid; a file with too few onsets for a
     * tempo estimate still keeps its measured onsets as snap magnets.
     */
    internal fun beatsFromEnvelope(energy: List<Float>, hopMs: Long): List<Long> {
        if (energy.isEmpty() || hopMs <= 0L) return emptyList()

        val flux = FloatArray(energy.size)
        // Start from the first window instead of from zero: a track that opens
        // on a steady tone must not look like an onset at 0 ms.
        var previous = energy[0].takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        var peak = 0f
        for (index in energy.indices) {
            val level = energy[index].takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
            val rise = if (index == 0) 0f else (level - previous).coerceAtLeast(0f)
            flux[index] = rise
            if (rise > peak) peak = rise
            previous = level
        }
        if (peak <= 1e-5f) return emptyList()
        for (index in flux.indices) flux[index] /= peak

        val onsets = BeatGrid.onsets(
            energy = flux,
            hopMs = hopMs,
            sensitivity = 1.2f,
            minGapMs = 120L
        )
        if (onsets.isEmpty()) return emptyList()

        val grid = BeatGrid.grid(onsets, energy.size * hopMs)
        return (grid?.beatsMs ?: onsets.map { it.atMs })
            .distinct()
            .sorted()
    }

    /**
     * Decodes the first audio track and returns its RMS envelope. The decoder
     * is streamed, so a long file never needs a decoded PCM buffer in memory.
     */
    private suspend fun energyEnvelope(context: Context, uri: Uri): List<Float>? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(index)
                val mime = candidate.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = index
                    format = candidate
                    break
                }
            }
            if (trackIndex < 0 || format == null) {
                AppLog.warn(TAG, "No audio track found in $uri")
                return null
            }
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            extractor.selectTrack(trackIndex)
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            val accumulator = EnvelopeAccumulator()
            accumulator.updateFormat(format)
            val info = MediaCodec.BufferInfo()
            var sawInputEnd = false
            var sawOutputEnd = false
            var idlePolls = 0

            while (!sawOutputEnd) {
                coroutineContext.ensureActive()

                if (!sawInputEnd) {
                    val inputIndex = decoder.dequeueInputBuffer(READ_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex)
                        val size = if (input == null) {
                            -1
                        } else {
                            runCatching { extractor.readSampleData(input, 0) }.getOrDefault(-1)
                        }
                        if (size < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEnd = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                size,
                                extractor.sampleTime.coerceAtLeast(0L),
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(info, READ_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        accumulator.updateFormat(decoder.outputFormat)
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        idlePolls++
                        // A stuck decoder must not spin forever on a broken file.
                        if (idlePolls > 500) break
                    }

                    else -> {
                        if (outputIndex < 0) break
                        idlePolls = 0
                        if (info.size > 0) {
                            decoder.getOutputBuffer(outputIndex)?.let { output ->
                                output.position(info.offset)
                                output.limit(info.offset + info.size)
                                accumulator.addPcm(output)
                            }
                        }
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEnd = true
                        }
                        if (accumulator.elapsedMs >= MAX_ANALYSIS_MS) sawOutputEnd = true
                    }
                }
            }

            accumulator.finish()
            if (accumulator.values.isEmpty()) {
                AppLog.warn(TAG, "No PCM was decoded for $uri")
                return null
            }
            return accumulator.values
        } catch (error: Exception) {
            AppLog.warn(TAG, "PCM decode failed for $uri", error)
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /** Streams decoded PCM into fixed-size RMS windows. */
    private class EnvelopeAccumulator {
        val values = ArrayList<Float>()

        private var sampleRate = 44_100
        private var channelCount = 1
        private var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
        private var hopSamples = (sampleRate * HOP_MS / 1_000L).toInt().coerceAtLeast(1)
        private var sumSquares = 0.0
        private var samplesInWindow = 0
        private var totalSamples = 0L

        val elapsedMs: Long
            get() = totalSamples * 1_000L / sampleRate.coerceAtLeast(1)

        fun updateFormat(format: MediaFormat) {
            sampleRate = readInt(format, MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                .coerceAtLeast(1)
            channelCount = readInt(format, MediaFormat.KEY_CHANNEL_COUNT, channelCount)
                .coerceAtLeast(1)
            pcmEncoding = readInt(
                format,
                MediaFormat.KEY_PCM_ENCODING,
                AudioFormat.ENCODING_PCM_16BIT
            )
            hopSamples = (sampleRate * HOP_MS / 1_000L).toInt().coerceAtLeast(1)
        }

        fun addPcm(buffer: ByteBuffer) {
            // duplicate() resets the order to BIG_ENDIAN; decoded PCM is native.
            val samples = buffer.duplicate().order(ByteOrder.nativeOrder())
            when (pcmEncoding) {
                AudioFormat.ENCODING_PCM_FLOAT -> {
                    val floats = samples.asFloatBuffer()
                    while (floats.remaining() >= channelCount) {
                        var frame = 0.0
                        repeat(channelCount) {
                            val value = floats.get().toDouble()
                            frame += value * value
                        }
                        addFrame(frame / channelCount)
                    }
                }

                else -> {
                    val shorts = samples.asShortBuffer()
                    while (shorts.remaining() >= channelCount) {
                        var frame = 0.0
                        repeat(channelCount) {
                            val value = shorts.get() / 32_768.0
                            frame += value * value
                        }
                        addFrame(frame / channelCount)
                    }
                }
            }
        }

        fun finish() {
            if (samplesInWindow > 0) {
                values.add((sumSquares / samplesInWindow).toFloat())
                sumSquares = 0.0
                samplesInWindow = 0
            }
        }

        private fun addFrame(square: Double) {
            sumSquares += square
            samplesInWindow++
            totalSamples++
            if (samplesInWindow >= hopSamples) {
                values.add((sumSquares / samplesInWindow).toFloat())
                sumSquares = 0.0
                samplesInWindow = 0
            }
        }

        private fun readInt(format: MediaFormat, key: String, fallback: Int): Int =
            if (format.containsKey(key)) {
                runCatching { format.getInteger(key) }.getOrDefault(fallback)
            } else {
                fallback
            }
    }
}
