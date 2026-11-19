package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The EQ value must be canonical before it reaches persistence or the DSP. */
class AudioEqualizerTest {

    @Test
    fun `default equalizer is flat`() {
        assertTrue(AudioEqualizer().isFlat)
        assertTrue(AudioEqualizer(0f, 0f, 0f).isFlat)
    }

    @Test
    fun `any band gain makes the equalizer active`() {
        assertFalse(AudioEqualizer(lowGainDb = 3f).isFlat)
        assertFalse(AudioEqualizer(midGainDb = -2f).isFlat)
        assertFalse(AudioEqualizer(highGainDb = 6f).isFlat)
    }

    @Test
    fun `gains are clamped to the supported decibel range`() {
        val clamped = AudioEqualizer(400f, -180f, 12.5f).sanitized()
        assertEquals(AudioEqualizer.MAX_GAIN_DB, clamped.lowGainDb, 0f)
        assertEquals(AudioEqualizer.MIN_GAIN_DB, clamped.midGainDb, 0f)
        assertEquals(AudioEqualizer.MAX_GAIN_DB, clamped.highGainDb, 0f)
    }

    @Test
    fun `non-finite gains fall back to flat`() {
        val clamped = AudioEqualizer(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
            .sanitized()
        assertEquals(AudioEqualizer(), clamped)
    }

    @Test
    fun `inaudible gains snap to exactly flat so saves stay canonical`() {
        val clamped = AudioEqualizer(0.01f, -0.02f, 0.049f).sanitized()
        assertTrue(clamped.isFlat)
        assertEquals(0f, clamped.lowGainDb, 0f)
        assertEquals(0f, clamped.midGainDb, 0f)
        assertEquals(0f, clamped.highGainDb, 0f)
    }

    @Test
    fun `sanitize is idempotent`() {
        val once = AudioEqualizer(20f, Float.NaN, 0.01f).sanitized()
        assertEquals(once, once.sanitized())
    }
}
