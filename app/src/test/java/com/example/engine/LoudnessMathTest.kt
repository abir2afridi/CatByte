package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** The loudness-normalisation maths behind the export "boost loudness" toggle. */
class LoudnessMathTest {

    private fun sineRmsDbfs(amplitude: Float, samples: Int = 10_000): Float {
        val data = FloatArray(samples) { i ->
            amplitude * Math.sin(2.0 * Math.PI * i / samples).toFloat()
        }
        return LoudnessMath.rmsDbfs(data)
    }

    @Test
    fun `full-scale sine measures near 0 dBFS`() {
        val dbfs = sineRmsDbfs(1f)
        assertEquals(-3.01f, dbfs, 0.05f) // RMS of a sine is A/sqrt(2)
    }

    @Test
    fun `digital silence reports the silence sentinel`() {
        assertEquals(-999f, LoudnessMath.rmsDbfs(FloatArray(1_000)), 1e-4f)
        assertEquals(-999f, LoudnessMath.rmsDbfs(FloatArray(0)), 1e-4f)
    }

    @Test
    fun `half amplitude is 6 dB quieter`() {
        val loud = sineRmsDbfs(1f)
        val quiet = sineRmsDbfs(0.5f)
        assertEquals(6.02f, loud - quiet, 0.05f)
    }

    @Test
    fun `normalisation gain reaches the -14 dBFS target`() {
        // A source measuring -20 dBFS needs +6 dB.
        assertEquals(6f, LoudnessMath.normaliseGainDb(-20f), 1e-4f)
        // A source at -8 dBFS is too loud: cut it by 6 dB.
        assertEquals(-6f, LoudnessMath.normaliseGainDb(-8f), 1e-4f)
        // Already at the target: no change.
        assertEquals(0f, LoudnessMath.normaliseGainDb(-14f), 1e-4f)
    }

    @Test
    fun `gains clamp and silence stays silent`() {
        assertEquals(12f, LoudnessMath.normaliseGainDb(-40f), 1e-4f)
        assertEquals(-6f, LoudnessMath.normaliseGainDb(-3f), 1e-4f)
        assertEquals(0f, LoudnessMath.normaliseGainDb(-999f), 1e-4f)
    }

    @Test
    fun `combine weights by duration and ignores silence`() {
        val combined = LoudnessMath.combineDbfs(
            listOf(-20f to 1_000L, -20f to 1_000L, -999f to 5_000L)
        )
        assertEquals(-20f, combined, 1e-4f) // the silent segment must not count
    }

    @Test
    fun `combine of two equal segments stays at their level`() {
        assertEquals(-20f, LoudnessMath.combineDbfs(listOf(-20f to 4_000L, -20f to 4_000L)), 1e-4f)
    }

    @Test
    fun `all silent measurements yield the silence sentinel`() {
        assertEquals(-999f, LoudnessMath.combineDbfs(listOf(-999f to 1_000L)), 1e-4f)
        assertEquals(-999f, LoudnessMath.combineDbfs(emptyList()), 1e-4f)
    }

    @Test
    fun `soft limiter passes the ceiling-free region untouched`() {
        assertEquals(0.5f, LoudnessMath.softLimit(0.5f), 1e-6f)
        assertEquals(-0.5f, LoudnessMath.softLimit(-0.5f), 1e-6f)
        assertEquals(0.95f, LoudnessMath.softLimit(0.95f), 1e-6f)
    }

    @Test
    fun `soft limiter folds peaks back under full scale smoothly`() {
        val limited = LoudnessMath.softLimit(1.3f)
        assertTrue(limited > LoudnessMath.LIMITER_CEILING) // still punchy
        assertTrue(limited < 1f)                            // but under clip
        assertEquals(-limited, LoudnessMath.softLimit(-1.3f), 1e-6f) // symmetric
        // Bigger overflow folds closer to, but never reaches, 1.
        assertTrue(LoudnessMath.softLimit(2f) > LoudnessMath.softLimit(1.3f))
        assertTrue(LoudnessMath.softLimit(2f) < 1f)
    }

    @Test
    fun `db to linear conversions`() {
        assertEquals(1f, LoudnessMath.dbToLinear(0f), 1e-6f)
        assertEquals(2f, LoudnessMath.dbToLinear(6.0206f), 1e-3f)
        assertEquals(0.5f, LoudnessMath.dbToLinear(-6.0206f), 1e-3f)
        assertEquals(1f, 1f * 10f.pow(0f / 20f), 1e-6f)
    }
}
