package com.example.data.captions

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.data.AppLog
import com.example.engine.Resampler
import java.nio.ByteOrder

/**
 * Decodes the audio track of a media file into 16 kHz mono PCM, the format
 * every Vosk model expects.
 *
 * Uses the platform MediaCodec directly (no ExoPlayer) so it can run against
 * any source URI - including a detached audio track that no longer shares a
 * player with the preview.
 */
object PcmExtractor {

    data class Result(val samples: ShortArray, val sampleRate: Int)

    /**
     * Decodes at most [maxDurationMs] of audio to mono 16 kHz, starting at
     * [startMs] inside the source file. Word timings are therefore relative to
     * [startMs] - callers pass the clip's trim start as the offset.
     * [outputSampleRate] stays 16 kHz: recognition happens at that rate.
     */
    fun extract(
        context: Context,
        uri: android.net.Uri,
        startMs: Long = 0L,
        maxDurationMs: Long = 10 * 60_000L,
        outputSampleRate: Int = 16_000
    ): Result {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrack = -1
            var format: MediaFormat? = null
            for (track in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(track)
                if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrack = track
                    format = trackFormat
                    break
                }
            }
            if (audioTrack < 0 || format == null) {
                return Result(ShortArray(0), outputSampleRate)
            }
            extractor.selectTrack(audioTrack)
            if (startMs > 0L) {
                extractor.seekTo(startMs * 1_000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            val inRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = format.getString(MediaFormat.KEY_MIME)!!

            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(format, null, null, 0)
                codec.start()

                val raw = ArrayList<Short>(256 * 1024)
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                val maxSamples = (maxDurationMs * inRate / 1000L).toInt() * channels

                while (!outputDone && raw.size < maxSamples) {
                    if (!inputDone) {
                        val inIndex = codec.dequeueInputBuffer(10_000L)
                        if (inIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inIndex)!!
                            val read = extractor.readSampleData(inputBuffer, 0)
                            if (read < 0) {
                                codec.queueInputBuffer(
                                    inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIndex, 0, read, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    when (val outIndex = codec.dequeueOutputBuffer(info, 10_000L)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // Rare mid-stream reconfiguration: log it and keep
                            // reading. The resampler tolerates the original
                            // rate, and real speech files do not flip rates.
                            val newFormat = codec.outputFormat
                            AppLog.warn(
                                TAG,
                                "Decoder changed output format: ${newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)}Hz"
                            )
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> {
                            val outBuffer = codec.getOutputBuffer(outIndex)!!
                            val shorts = outBuffer.order(ByteOrder.LITTLE_ENDIAN)
                                .asShortBuffer()
                            val limit = shorts.remaining()
                            for (i in 0 until limit) raw.add(shorts.get(i))
                            codec.releaseOutputBuffer(outIndex, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
                return Result(
                    Resampler.toMonoAtRate(raw.toShortArray(), channels, inRate, outputSampleRate),
                    outputSampleRate
                )
            } finally {
                try {
                    codec.stop()
                    codec.release()
                } catch (ignored: IllegalStateException) {
                }
            }
        } finally {
            extractor.release()
        }
    }

    private const val TAG = "PcmExtractor"
}
