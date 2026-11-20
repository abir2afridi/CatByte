package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Decodes the audio track of a media URI into raw mono PCM floats.
 *
 * [MediaWaveform] only keeps a coarse envelope; the AI Lab's beat finder needs
 * the actual samples, so this runs the same MediaCodec path but keeps every
 * sample it can afford to hold. [maxDurationMs] caps the work so a long video
 * cannot freeze the screen or exhaust memory.
 */
internal object PcmDecoder {

    data class DecodedAudio(
        /** Mono samples normalised to -1..1. */
        val samples: FloatArray,
        val sampleRate: Int,
        val durationMs: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DecodedAudio) return false
            return sampleRate == other.sampleRate &&
                durationMs == other.durationMs &&
                samples.contentEquals(other.samples)
        }

        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = 31 * result + sampleRate
            result = 31 * result + durationMs.hashCode()
            return result
        }
    }

    /** Hard cap on the decoded length so the UI never blocks on a huge file. */
    private const val DEFAULT_MAX_DURATION_MS = 120_000L

    /** Decodes [uri] and returns null when it has no usable audio track. */
    suspend fun decode(
        context: Context,
        uri: Uri,
        maxDurationMs: Long = DEFAULT_MAX_DURATION_MS
    ): DecodedAudio? = withContext(Dispatchers.IO) {
        runCatching { decodeInternal(context, uri, maxDurationMs) }
            .onFailure { AppLog.warn("PcmDecoder", "PCM decode failed for $uri", it) }
            .getOrNull()
    }

    private fun decodeInternal(context: Context, uri: Uri, maxDurationMs: Long): DecodedAudio? {
        // SAF grants are not persisted by default; the waveform path already does
        // this and it keeps re-picks working after the activity is recreated.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

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
                AppLog.warn("PcmDecoder", "No audio track found for $uri")
                return null
            }

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            val limitUs = maxDurationMs.coerceAtLeast(1_000L) * 1_000L
            val decodeUs = if (durationUs > 0L) minOf(durationUs, limitUs) else limitUs

            extractor.selectTrack(trackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            val expectedSamples = (decodeUs * sampleRate / 1_000_000L)
                .coerceAtLeast(1L)
                .toInt()
            val decoded = ArrayList<Float>(expectedSamples)
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            var maxSamples = Int.MAX_VALUE

            while (!sawOutputEos && decoded.size < maxSamples) {
                if (!sawInputEos) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        val sampleSize = try {
                            if (inputBuffer != null) extractor.readSampleData(inputBuffer, 0) else -1
                        } catch (_: Exception) {
                            -1
                        }
                        if (sampleSize < 0 || extractor.sampleTime > decodeUs) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outputIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            decodeMono(outputBuffer, channelCount, decoded)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEos = true
                        }
                    }
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && sawInputEos -> continue
                }
            }

            if (decoded.isEmpty()) {
                AppLog.warn("PcmDecoder", "No PCM decoded for $uri")
                return null
            }
            maxSamples = decoded.size
            val durationMs = decoded.size * 1_000L / sampleRate
            return DecodedAudio(
                samples = FloatArray(decoded.size) { decoded[it] },
                sampleRate = sampleRate,
                durationMs = durationMs
            )
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /**
     * Converts a 16-bit interleaved buffer into mono floats by averaging the
     * channels of each frame.
     */
    private fun decodeMono(
        buffer: ByteBuffer,
        channelCount: Int,
        out: ArrayList<Float>
    ) {
        val bytes = buffer.remaining()
        val shorts = bytes / 2
        if (shorts <= 0) return
        val channels = channelCount.coerceAtLeast(1)
        val frames = shorts / channels
        val shortBuffer = buffer.asShortBuffer()
        for (frame in 0 until frames) {
            var sum = 0f
            for (channel in 0 until channels) {
                if (shortBuffer.remaining() <= 0) break
                sum += shortBuffer.get().toInt() / 32_768f
            }
            out += sum / channels
        }
    }
}
