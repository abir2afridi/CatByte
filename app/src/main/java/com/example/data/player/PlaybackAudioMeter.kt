package com.example.data.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

/**
 * Live peak levels of the audio that is actually leaving the preview player.
 *
 * A [PeakProcessor] sits at the end of the sink's audio-processor chain, so
 * every sample that reaches the speaker (after clip volume, fades, solo/mute,
 * speed and the mix) passes through it. The UI draws these peaks instead of a
 * decorative animation. This file stays Android-free so the maths can run in a
 * plain JVM unit test; the sink wiring lives in [PreviewAudioSink].
 */
@UnstableApi
object PlaybackAudioMeter {

    /** Peak amplitude per channel, 0..1. */
    data class Levels(val left: Float = 0f, val right: Float = 0f) {
        val peak: Float get() = max(left, right)
        val isSilent: Boolean get() = peak <= SILENCE_THRESHOLD
    }

    private const val SILENCE_THRESHOLD = 0.0001f

    /** How often a new peak is published, so the UI recomposes at most ~20x/s. */
    const val PUBLISH_INTERVAL_NANOS = 50_000_000L

    private val _levels = MutableStateFlow(Levels())
    val levels: StateFlow<Levels> = _levels.asStateFlow()

    /** Drops the meter to silence (pause, seek, release, muted preview). */
    internal fun reset() {
        _levels.value = Levels()
    }

    /** Publishes a measured peak; the processor calls this. */
    internal fun publish(left: Float, right: Float) {
        _levels.value = Levels(left.coerceIn(0f, 1f), right.coerceIn(0f, 1f))
    }

    /**
     * Taps interleaved PCM and publishes the loudest sample of each channel.
     *
     * The processor never alters the audio: the input buffer is copied to the
     * output untouched. Formats it cannot read are passed through without
     * measuring rather than failing the sink.
     */
    internal class PeakProcessor(
        private val publishIntervalNanos: Long = PUBLISH_INTERVAL_NANOS,
        private val clock: () -> Long = System::nanoTime
    ) : BaseAudioProcessor() {

        private var lastPublishNanos = 0L
        private var pendingLeft = 0f
        private var pendingRight = 0f

        override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
            inputAudioFormat

        override fun onFlush(streamMetadata: StreamMetadata) {
            // A seek flushes the graph; the old peak must not linger.
            pendingLeft = 0f
            pendingRight = 0f
            PlaybackAudioMeter.reset()
        }

        override fun onReset() {
            pendingLeft = 0f
            pendingRight = 0f
            PlaybackAudioMeter.reset()
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val remaining = inputBuffer.remaining()
            if (remaining > 0) {
                val format = inputAudioFormat
                // duplicate() resets the byte order to BIG_ENDIAN; the PCM
                // samples are native-order, so keep the source order.
                val samples = inputBuffer.duplicate().order(inputBuffer.order())
                when (format.encoding) {
                    C.ENCODING_PCM_16BIT -> measure16(samples, format.channelCount)
                    C.ENCODING_PCM_FLOAT -> measureFloat(samples, format.channelCount)
                    else -> Unit
                }
                val output = replaceOutputBuffer(remaining)
                output.put(inputBuffer)
                output.flip()
            }
        }

        private fun measure16(buffer: ByteBuffer, channels: Int) {
            if (channels <= 0) return
            val shorts = buffer.asShortBuffer()
            val frames = shorts.remaining() / channels
            if (frames == 0) return
            var left = 0f
            var right = 0f
            for (frame in 0 until frames) {
                for (channel in 0 until channels) {
                    val sample = abs(shorts.get().toInt()) / 32768f
                    if (channel == 0) left = max(left, sample)
                    if (channel == 1) right = max(right, sample)
                }
            }
            accumulate(left, if (channels == 1) left else right)
        }

        private fun measureFloat(buffer: ByteBuffer, channels: Int) {
            if (channels <= 0) return
            val floats = buffer.asFloatBuffer()
            val frames = floats.remaining() / channels
            if (frames == 0) return
            var left = 0f
            var right = 0f
            for (frame in 0 until frames) {
                for (channel in 0 until channels) {
                    val sample = abs(floats.get()).coerceAtMost(1f)
                    if (channel == 0) left = max(left, sample)
                    if (channel == 1) right = max(right, sample)
                }
            }
            accumulate(left, if (channels == 1) left else right)
        }

        private fun accumulate(left: Float, right: Float) {
            pendingLeft = max(pendingLeft, left)
            pendingRight = max(pendingRight, right)
            val now = clock()
            if (now - lastPublishNanos < publishIntervalNanos) return
            lastPublishNanos = now
            PlaybackAudioMeter.publish(pendingLeft, pendingRight)
            pendingLeft = 0f
            pendingRight = 0f
        }
    }
}
