package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Beat detection maths. These tests feed an RMS envelope directly, so the
 * onset/tempo path is verified without a device or a codec.
 */
class BeatDetectorTest {

    private fun envelope(
        durationMs: Long,
        hopMs: Long = BeatDetector.HOP_MS,
        pulsesAtMs: List<Long> = emptyList(),
        baseline: Float = 0.02f
    ): List<Float> {
        val size = (durationMs / hopMs).toInt()
        val values = FloatArray(size) { baseline }
        for (pulse in pulsesAtMs) {
            val index = (pulse / hopMs).toInt()
            if (index !in values.indices) continue
            values[index] = 0.9f
            // A real hit decays over the next windows instead of vanishing.
            if (index + 1 < values.size) values[index + 1] = 0.45f
            if (index + 2 < values.size) values[index + 2] = 0.2f
        }
        return values.toList()
    }

    @Test
    fun `a pulse train becomes a regular beat grid`() {
        val beats = BeatDetector.beatsFromEnvelope(
            envelope(
                durationMs = 12_000L,
                pulsesAtMs = (500L..11_500L step 500L).toList()
            ),
            BeatDetector.HOP_MS
        )

        assertTrue("expected a full grid, got $beats", beats.size >= 20)
        assertTrue("first beat should sit near 500 ms: ${beats.first()}", beats.first() in 440L..560L)
        val intervals = beats.zipWithNext { a, b -> b - a }
        assertTrue(
            "beat intervals should be steady: $intervals",
            intervals.all { it in 440L..560L }
        )
    }

    @Test
    fun `too few onsets for a tempo still keeps the measured onsets`() {
        val beats = BeatDetector.beatsFromEnvelope(
            envelope(
                durationMs = 4_000L,
                pulsesAtMs = listOf(500L, 1_000L, 1_500L)
            ),
            BeatDetector.HOP_MS
        )

        assertEquals(3, beats.size)
        assertTrue("onsets should stay where they were measured: $beats", beats[0] in 440L..560L)
        assertTrue("onsets should stay where they were measured: $beats", beats[1] in 940L..1_060L)
        assertTrue("onsets should stay where they were measured: $beats", beats[2] in 1_440L..1_560L)
    }

    @Test
    fun `silence has no beats`() {
        val beats = BeatDetector.beatsFromEnvelope(
            envelope(durationMs = 6_000L, baseline = 0f),
            BeatDetector.HOP_MS
        )

        assertTrue(beats.isEmpty())
    }

    @Test
    fun `a steady tone has no onsets`() {
        val steady = List(500) { 0.3f }

        assertTrue(BeatDetector.beatsFromEnvelope(steady, BeatDetector.HOP_MS).isEmpty())
    }

    @Test
    fun `empty or invalid envelopes are rejected`() {
        assertTrue(BeatDetector.beatsFromEnvelope(emptyList(), 20L).isEmpty())
        assertTrue(BeatDetector.beatsFromEnvelope(listOf(0.5f), 0L).isEmpty())
    }
}
