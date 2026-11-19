package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The compressor value must be canonical before it reaches persistence or the DSP. */
class AudioCompressorTest {

    @Test
    fun `a disabled compressor costs no processor`() {
        assertTrue(AudioCompressor().isNoOp)
        assertFalse(AudioCompressor(enabled = true).isNoOp)
    }

    @Test
    fun `parameters are clamped to the supported ranges`() {
        val clamped = AudioCompressor(
            enabled = true,
            thresholdDb = 40f,
            ratio = 500f,
            makeupGainDb = -6f,
            attackMs = 0f,
            releaseMs = 99_999f
        ).sanitized()

        assertEquals(AudioCompressor.MAX_THRESHOLD_DB, clamped.thresholdDb, 0f)
        assertEquals(AudioCompressor.MAX_RATIO, clamped.ratio, 0f)
        assertEquals(AudioCompressor.MIN_MAKEUP_GAIN_DB, clamped.makeupGainDb, 0f)
        assertEquals(AudioCompressor.MIN_ATTACK_MS, clamped.attackMs, 0f)
        assertEquals(AudioCompressor.MAX_RELEASE_MS, clamped.releaseMs, 0f)
    }

    @Test
    fun `non-finite parameters fall back to the defaults`() {
        val clamped = AudioCompressor(
            enabled = true,
            thresholdDb = Float.NaN,
            ratio = Float.POSITIVE_INFINITY,
            makeupGainDb = Float.NEGATIVE_INFINITY,
            attackMs = Float.NaN,
            releaseMs = Float.NaN
        ).sanitized()

        assertEquals(AudioCompressor.DEFAULT_THRESHOLD_DB, clamped.thresholdDb, 0f)
        assertEquals(AudioCompressor.DEFAULT_RATIO, clamped.ratio, 0f)
        assertEquals(0f, clamped.makeupGainDb, 0f)
        assertEquals(AudioCompressor.DEFAULT_ATTACK_MS, clamped.attackMs, 0f)
        assertEquals(AudioCompressor.DEFAULT_RELEASE_MS, clamped.releaseMs, 0f)
    }

    @Test
    fun `sanitize is idempotent`() {
        val once = AudioCompressor(enabled = true, thresholdDb = 900f, ratio = 0.1f).sanitized()
        assertEquals(once, once.sanitized())
    }

    @Test
    fun `the curve leaves quiet levels untouched`() {
        assertEquals(
            0f,
            AudioCompressor.gainReductionDb(levelDb = -40f, thresholdDb = -18f, ratio = 4f),
            0.0001f
        )
    }

    @Test
    fun `the curve compresses loud levels by the ratio`() {
        // 12 dB over a -18 dB threshold with a 4:1 ratio: 12 * (1 - 1/4) = 9 dB.
        val reduction = AudioCompressor.gainReductionDb(
            levelDb = -6f,
            thresholdDb = -18f,
            ratio = 4f,
            kneeDb = 0f
        )
        assertEquals(9f, reduction, 0.0001f)
    }

    @Test
    fun `a ratio of one never attenuates`() {
        assertEquals(
            0f,
            AudioCompressor.gainReductionDb(levelDb = 0f, thresholdDb = -18f, ratio = 1f),
            0.0001f
        )
    }

    @Test
    fun `the soft knee bends smoothly between the two slopes`() {
        val knee = AudioCompressor.KNEE_DB
        val threshold = -18f
        val ratio = 4f
        val atEdge = AudioCompressor.gainReductionDb(
            levelDb = threshold - knee / 2f,
            thresholdDb = threshold,
            ratio = ratio
        )
        val inKnee = AudioCompressor.gainReductionDb(
            levelDb = threshold,
            thresholdDb = threshold,
            ratio = ratio
        )
        val aboveKnee = AudioCompressor.gainReductionDb(
            levelDb = threshold + knee / 2f,
            thresholdDb = threshold,
            ratio = ratio
        )
        assertEquals(0f, atEdge, 0.0001f)
        assertTrue("the knee must attenuate", inKnee > 0f)
        assertTrue("the knee must be gentler than the straight line", inKnee < aboveKnee)
    }

    @Test
    fun `the compressor never adds gain of its own`() {
        for (level in -60..6) {
            val reduction = AudioCompressor.gainReductionDb(
                levelDb = level.toFloat(),
                thresholdDb = -18f,
                ratio = 8f
            )
            assertTrue("reduction at $level dB was $reduction", reduction >= -0.0001f)
        }
    }
}
