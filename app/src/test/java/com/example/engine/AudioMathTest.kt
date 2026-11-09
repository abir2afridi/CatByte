package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioMathTest {

    private fun sine(amplitude: Float, samples: Int = 480): FloatArray =
        FloatArray(samples) { index ->
            (amplitude * sin(2.0 * PI * index / 48.0)).toFloat()
        }

    @Test
    fun `fades ramp in and out`() {
        assertEquals(0f, AudioMath.fadeGain(0L, 1_000L, 100L, 100L), 0.001f)
        assertEquals(0.5f, AudioMath.fadeGain(50L, 1_000L, 100L, 100L), 0.001f)
        assertEquals(1f, AudioMath.fadeGain(500L, 1_000L, 100L, 100L), 0.001f)
        assertEquals(0.5f, AudioMath.fadeGain(950L, 1_000L, 100L, 100L), 0.001f)
        assertEquals(0f, AudioMath.fadeGain(0L, 0L), 0.001f)
    }

    @Test
    fun `limiter only touches peaks over the ceiling`() {
        assertEquals(1f, AudioMath.limiterGain(0.5f), 0.001f)
        assertEquals(0.49f, AudioMath.limiterGain(2f), 0.001f)
        assertEquals(1f, AudioMath.limiterGain(0f), 0.001f)
    }

    @Test
    fun `mixing two loud tracks is limited instead of clipped`() {
        val mixed = AudioMath.mixTracks(
            tracks = listOf(FloatArray(4) { 0.6f }, FloatArray(4) { 0.6f }),
            gains = listOf(1f, 1f)
        )
        assertEquals(0.98f, mixed[0], 0.01f)
        assertTrue(AudioMath.isHeadroomOk(mixed))
    }

    @Test
    fun `mixing respects per track gain`() {
        val mixed = AudioMath.mixTracks(
            tracks = listOf(FloatArray(2) { 0.4f }, FloatArray(2) { 0.4f }),
            gains = listOf(1f, 0f)
        )
        assertEquals(0.4f, mixed[0], 0.001f)
    }

    @Test
    fun `rms of a full scale sine is minus three dbfs`() {
        assertEquals(-3.01f, AudioMath.rmsDbfs(sine(1f)), 0.1f)
        assertEquals(-90f, AudioMath.rmsDbfs(FloatArray(0)), 0.001f)
        assertEquals(-90f, AudioMath.rmsDbfs(FloatArray(8) { 0f }), 0.001f)
    }

    @Test
    fun `loudness normalisation boosts quiet audio only`() {
        val quiet = sine(0.1f)
        assertTrue(AudioMath.normalisationGain(quiet) > 1f)
        val loud = sine(1f)
        assertTrue(AudioMath.normalisationGain(loud) <= 1f)
        assertEquals(AudioMath.rmsDbfs(loud), AudioMath.approxLufs(loud), 0.001f)
    }

    @Test
    fun `ducking follows the voice range`() {
        val ranges = listOf(400L..900L)
        assertEquals(0.65f, AudioMath.duckEnvelope(500L, ranges), 0.001f)
        assertEquals(1f, AudioMath.duckEnvelope(0L, ranges), 0.001f)
        assertEquals(1f, AudioMath.duckEnvelope(0L, emptyList()), 0.001f)
        assertTrue(AudioMath.duckEnvelope(350L, ranges) < 1f)
    }

    @Test
    fun `pcm round trips within sixteen bit precision`() {
        val samples = floatArrayOf(0.5f, -0.5f, 0f, 0.25f)
        val roundTrip = AudioMath.pcm16ToFloat(AudioMath.floatToPcm16(samples))
        assertEquals(samples.size, roundTrip.size)
        samples.indices.forEach { index ->
            assertEquals(samples[index], roundTrip[index], 0.001f)
        }
    }

    @Test
    fun `resampling halves the sample count`() {
        val resampled = AudioMath.resample(FloatArray(100) { it / 100f }, 48_000, 24_000)
        assertEquals(50, resampled.size)
    }

    @Test
    fun `peak normalise lands on the ceiling`() {
        val normalised = AudioMath.peakNormalise(floatArrayOf(2f, -1f))
        assertEquals(0.98f, AudioMath.peakOf(normalised), 0.01f)
    }

    @Test
    fun `low pass keeps length and reduces level`() {
        val filtered = AudioMath.lowPass(sine(1f), cutoffHz = 200f, sampleRate = 48_000)
        assertEquals(480, filtered.size)
        assertTrue(AudioMath.rms(filtered) <= AudioMath.rms(sine(1f)))
    }
}
