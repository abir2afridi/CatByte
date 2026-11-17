package com.example.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Media3 addresses the gain providers in SAMPLE FRAMES, not in microseconds.
 *
 * These tests push real PCM through the actual [androidx.media3.common.audio.GainProcessor],
 * so a fade that is stretched by the sample rate (48x at 48 kHz) or a duck
 * that never arrives fails here instead of shipping in the preview and the
 * export.
 */
class GainProviderTimingTest {

    private val sampleRate = GainProbe.SAMPLE_RATE

    @Test
    fun `fade in reaches full gain after the requested milliseconds`() {
        val values = GainProbe.render(
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
        assertEquals(0.5f, values[GainProbe.sampleAt(0.5f)], 0.01f)
        // At the end of the fade the signal is at full level.
        assertEquals(1f, values[GainProbe.sampleAt(1.0f)], 0.01f)
    }

    @Test
    fun `fade out drops the gain over the requested milliseconds`() {
        val values = GainProbe.render(
            ConstantGainProvider(
                gain = 1f,
                fadeInMs = 0L,
                fadeOutMs = 1_000L,
                durationMs = 5_000L
            ),
            seconds = 5
        )

        assertEquals(1f, values[GainProbe.sampleAt(3.5f)], 0.01f)
        assertEquals(0.5f, values[GainProbe.sampleAt(4.5f)], 0.01f)
        // The last sample of the clip is silent.
        assertEquals(0f, values[values.size - 1], 0.01f)
    }

    @Test
    fun `music dips under a voice-over and comes back afterwards`() {
        val values = GainProbe.render(
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
        assertEquals(0.5f, values[GainProbe.sampleAt(1.5f)], 0.02f)
        // Well after the voice the music is back at full level.
        assertEquals(1f, values[GainProbe.sampleAt(3.0f)], 0.02f)
    }

    @Test
    fun `a muted track stays silent`() {
        val values = GainProbe.render(
            ConstantGainProvider(
                gain = 0f,
                fadeInMs = 0L,
                fadeOutMs = 0L,
                durationMs = 2_000L
            ),
            seconds = 1
        )

        assertEquals(sampleRate, values.size)
        assertEquals(0f, values[GainProbe.sampleAt(0.5f)], 0.0001f)
    }
}
