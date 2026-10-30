package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Reads real information about the media files the user imports: the true
 * duration and an actual waveform (not a placeholder).
 */
object MediaMetadata {

    /** Maximum amount of audio (in milliseconds) that is analysed for the waveform. */
    private const val WAVEFORM_ANALYSIS_LIMIT_MS = 90_000L

    /** Returns the duration of the media in milliseconds, or 0 when unknown. */
    suspend fun durationMs(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        // Persist URI permission for future access
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            AppLog.warn("MediaMetadata", "Failed to persist URI permission", e)
        }
        
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                runCatching { retriever.release() }
            }
        }.onFailure { e ->
            AppLog.warn("MediaMetadata", "Failed to get duration for $uri", e)
        }.getOrDefault(0L)
    }

    /** Returns a human readable name for the media. */
    suspend fun displayName(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        // Persist URI permission for future access
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            AppLog.warn("MediaMetadata", "Failed to persist URI permission", e)
        }
        
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            } finally {
                runCatching { retriever.release() }
            }
        }.onFailure { e ->
            AppLog.warn("MediaMetadata", "Failed to get display name for $uri", e)
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment.orEmpty().substringBeforeLast('.')
    }

    /**
     * Builds a real waveform for the audio of [uri]. The returned list contains
     * [buckets] normalised values between 0 and 1. Returns null when the audio
     * could not be decoded.
     */
    suspend fun waveform(context: Context, uri: Uri, buckets: Int = 48): List<Float>? =
        withContext(Dispatchers.IO) {
            runCatching { decodeWaveform(context, uri, buckets) }.getOrNull()
        }

    private fun decodeWaveform(context: Context, uri: Uri, buckets: Int): List<Float>? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            // Ensure we have read permission for the URI
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            extractor.setDataSource(context, uri, null)

            var trackIndex = -1
            var format: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(index)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = index
                    format = trackFormat
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = format.getLong(MediaFormat.KEY_DURATION, 0L)
            if (durationUs <= 0L) return null

            val analysisUs = minOf(durationUs, WAVEFORM_ANALYSIS_LIMIT_MS * 1000L)
            val bucketUs = analysisUs / buckets.coerceAtLeast(1)

            val peaks = FloatArray(buckets.coerceAtLeast(1))
            val bucketHits = IntArray(buckets.coerceAtLeast(1))

            extractor.selectTrack(trackIndex)
            val decoder = MediaCodec.createDecoderByType(mime)
            if (decoder == null) {
                AppLog.warn("MediaMetadata", "No decoder found for mime: $mime")
                return null
            }
            codec = decoder
            try {
                codec.configure(format, null, null, 0)
            } catch (e: Exception) {
                AppLog.warn("MediaMetadata", "Codec configure failed for $mime", e)
                return null
            }
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false

            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        val sampleSize = if (inputBuffer != null) {
                            try {
                                extractor.readSampleData(inputBuffer, 0)
                            } catch (e: Exception) {
                                -1
                            }
                        } else {
                            -1
                        }
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
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
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        accumulate(peaks, bucketHits, bucketUs, bufferInfo.presentationTimeUs, outputBuffer)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && sawInputEos) {
                    continue
                }
            }

            if (bucketHits.sum() == 0) return null
            val maxPeak = peaks.maxOrNull() ?: 0f
            if (maxPeak <= 0f) return null
            return peaks.map { (it / maxPeak).coerceIn(0f, 1f) }
        } catch (e: Exception) {
            AppLog.warn("MediaMetadata", "Waveform decode failed for $uri", e)
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun accumulate(
        peaks: FloatArray,
        bucketHits: IntArray,
        bucketUs: Long,
        presentationTimeUs: Long,
        buffer: ByteBuffer
    ) {
        val bytes = buffer.remaining()
        val samples = bytes / 2
        if (samples <= 0) return
        var sum = 0f
        var count = 0
        val shorts = buffer.asShortBuffer()
        while (shorts.remaining() >= 2) {
            val sample = shorts.get().toInt()
            sum += kotlin.math.abs(sample) / 32768f
            count++
            if (count >= 2048) {
                val average = sum / count
                val bucket = ((presentationTimeUs / bucketUs).toInt()).coerceIn(0, peaks.size - 1)
                if (average > peaks[bucket]) peaks[bucket] = average
                bucketHits[bucket]++
                sum = 0f
                count = 0
            }
        }
        if (count > 0) {
            val average = sum / count
            val bucket = ((presentationTimeUs / bucketUs).toInt()).coerceIn(0, peaks.size - 1)
            if (average > peaks[bucket]) peaks[bucket] = average
            bucketHits[bucket]++
        }
    }
}
