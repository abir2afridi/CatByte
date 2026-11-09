package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioLoudnessTest {

    @Test
    fun `empty waveform reports zero rms and unity gain`() {
        assertEquals(0f, AudioLoudness.rms(emptyList()), 0.0001f)
        assertEquals(1f, AudioLoudness.gainFor(emptyList()), 0.0001f)
        assertFalse(AudioLoudness.canNormalise(emptyList()))
    }

    @Test
    fun `silent waveform cannot be normalised`() {
        val silence = List(32) { 0f }
        assertEquals(0f, AudioLoudness.rms(silence), 0.0001f)
        assertFalse(AudioLoudness.canNormalise(silence))
        assertEquals(1f, AudioLoudness.gainFor(silence), 0.0001f)
    }

    @Test
    fun `quiet clip is boosted toward the target`() {
        // 0.1 RMS needs exactly +6 dB, comfortably below the +12 dB cap.
        val quiet = List(64) { 0.1f }
        val gain = AudioLoudness.gainFor(quiet)
        assertEquals(0.2f / 0.1f, gain, 0.001f)
        assertTrue(gain > 1f)
    }

    @Test
    fun `boost is capped at twelve db`() {
        val veryQuiet = List(64) { 0.001f }
        val gain = AudioLoudness.gainFor(veryQuiet)
        assertEquals(3.981f, gain, 0.01f)
    }

    @Test
    fun `loud clip is attenuated but never below the floor`() {
        val loud = List(64) { 1f }
        assertEquals(AudioLoudness.MIN_GAIN, AudioLoudness.gainFor(loud), 0.0001f)
        assertEquals(1f, AudioLoudness.rms(loud), 0.0001f)
    }

    @Test
    fun `clip already at target is left untouched`() {
        val target = List(48) { AudioLoudness.DEFAULT_TARGET_RMS }
        assertEquals(1f, AudioLoudness.gainFor(target), 0.0001f)
        assertTrue(AudioLoudness.canNormalise(target))
    }

    @Test
    fun `non-finite samples are ignored`() {
        val waveform = listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.5f, 0.2f, 0.4f)
        val rms = AudioLoudness.rms(waveform)
        val expected = kotlin.math.sqrt((0.2f * 0.2f + 0.4f * 0.4f) / 2.0).toFloat()
        assertEquals(expected, rms, 0.0001f)
    }
}
