package com.example.data.export

import androidx.media3.common.AudioFormat
import androidx.media3.common.C
import androidx.media3.common.audio.GainProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 addresses the gain providers in SAMPLE FRAMES, not in microseconds.
 *
 * These tests push real PCM through the actual [GainProcessor], so a fade
 * that is stretched by the sample rate (48x at 48 kHz) or a duck that never
 * arrives fails here instead of shipping in the preview and the export.
 */
class GainProviderTimingTest {

    private val sampleRate = 48_000

    /** Renders one second of full-scale PCM through the provider. */
    private fun render(provider: GainProcessor.GainProvider, seconds: Int): FloatArray {
        val processor = GainProcessor(provider)
        processor.configure(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelCount(1)
                .setEncoding(C.ENCODING_PCM_FLOAT)
                .build()
        )
        val frameCount = sampleRate * seconds
        val input = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.nativeOrder())
        repeat(frameCount) { input.putFloat(1f) }
        input.flip()
        processor.queueInput(input)
        val output = processor.getOutput() ?: return FloatArray(0)
        val floats = output.order(ByteOrder.nativeOrder()).asFloatBuffer()
        val values = FloatArray(floats.remaining())
        floats.get(values)
        return values
    }

    private fun sampleAt(seconds: Float): Int = (sampleRate * seconds).toInt()

    @Test
    fun `fade in reaches full gain after the requested milliseconds`() {
        val values = render(
            ConstantGainProvider(
                gain = 1f,
                fadeInMs = 1_000L,
                fadeOutMs = 0L,
                durationMs = 5_000L
            ),
            seconds = 2
        )

        assertEquals(sampleRate * 2, values.size)
        // Halfway through the fade the gain is halfway too.
        assertEquals(0.5f, values[sampleAt(0.5f)], 0.01f)
        // At the end of the fade the signal is at full level.
        assertEquals(1f, values[sampleAt(1.0f)], 0.01f)
    }

    @Test
    fun `fade out drops the gain over the requested milliseconds`() {
        val values = render(
            ConstantGainProvider(
                gain = 1f,
                fadeInMs = 0L,
                fadeOutMs = 1_000L,
                durationMs = 5_000L
            ),
            seconds = 5
        )

        assertEquals(1f, values[sampleAt(3.5f)], 0.01f)
        assertEquals(0.5f, values[sampleAt(4.5f)], 0.01f)
        // The last sample of the clip is silent.
        assertEquals(0f, values[values.size - 1], 0.01f)
    }

    @Test
    fun `music dips under a voice-over and comes back afterwards`() {
        val values = render(
            DuckGainProvider(
                base = ConstantGainProvider(
                    gain = 1f,
                    fadeInMs = 0L,
                    fadeOutMs = 0L,
                    durationMs = 5_000L
                ),
                startTimelineMs = 0L,
                voiceRanges = listOf(1_000L..2_000L),
                duckAmount = 0.5f
            ),
            seconds = 4
        )

        // Halfway through the voice the music sits at half its level.
        assertEquals(0.5f, values[sampleAt(1.5f)], 0.02f)
        // Well after the voice the music is back at full level.
        assertEquals(1f, values[sampleAt(3.0f)], 0.02f)
    }

    @Test
    fun `a muted track stays silent`() {
        val values = render(
            ConstantGainProvider(
                gain = 0f,
                fadeInMs = 0L,
                fadeOutMs = 0L,
                durationMs = 2_000L
            ),
            seconds = 1
        )

        assertEquals(sampleRate, values.size)
        assertEquals(0f, values[sampleAt(0.5f)], 0.0001f)
    }
}
