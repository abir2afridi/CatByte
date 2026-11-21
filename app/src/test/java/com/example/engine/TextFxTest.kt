package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shared text-animation maths both renderers consume. Preview and export
 * MUST evaluate identical values - these tests pin the curves so neither
 * renderer can fork them again.
 */
class TextFxTest {

    @Test
    fun `typewriter reveals chars proportionally and never past the end`() {
        assertEquals(2, TextFx.typewriterChars("abcdef", 0.25f)) // 6 * 0.25 * 1.6 = 2.4 -> 2
        assertEquals(6, TextFx.typewriterChars("abcdef", 1f))
        assertEquals(1, TextFx.typewriterChars("abcdef", 0f))
        assertEquals(0, TextFx.typewriterChars("", 1f))
    }

    @Test
    fun `pop up scale springs from half to one`() {
        assertEquals(0.6f, TextFx.popUpScale(0L), 1e-4f)
        assertEquals(1f, TextFx.popUpScale(1_000L), 1e-4f)
        assertTrue(TextFx.popUpScale(160L) in 0.6f..1f)
    }

    @Test
    fun `fade in alpha clamps to unit range`() {
        assertEquals(0f, TextFx.fadeInAlpha(0L), 1e-4f)
        assertEquals(1f, TextFx.fadeInAlpha(10_000L), 1e-4f)
        assertTrue(TextFx.fadeInAlpha(160L) in 0f..1f)
    }

    @Test
    fun `neon pulse breathes within its band`() {
        assertTrue(TextFx.neonPulseAlpha(0L) >= TextFx.NEON_PULSE_BASE)
        assertTrue(TextFx.neonPulseAlpha(105L) <= TextFx.NEON_PULSE_BASE + TextFx.NEON_PULSE_RANGE)
        assertTrue(TextFx.neonPulseAlpha(2_000L) >= TextFx.NEON_PULSE_BASE - TextFx.NEON_PULSE_RANGE)
    }

    @Test
    fun `wave offset scales with the canvas height`() {
        val small = TextFx.waveOffsetY(63L, 100f)
        val large = TextFx.waveOffsetY(63L, 1_000f)
        // Both are peaks of the same sine, so the ratio is the height ratio.
        assertEquals(large / small, 10f, 0.05f)
        // A quarter period later the offset swings the other way.
        val opposite = TextFx.waveOffsetY(63L + (TextFx.WAVE_PERIOD_MS / 2).toLong(), 1_000f)
        assertTrue(small * opposite < 0f || abs(small - opposite) > 0.001f)
    }

    @Test
    fun `title size is the width fraction`() {
        assertEquals(1_080f * 0.085f, TextFx.titleTextSizePx(1_080f), 1e-4f) // 91.8
        assertEquals(720f * 0.085f, TextFx.titleTextSizePx(720f), 1e-4f)     // 61.2
    }

    @Test
    fun `letter spacing converts em to pixels`() {
        assertEquals(108f * 0.08f, TextFx.letterSpacingPx(108f, 0.08f), 1e-2f)
        assertEquals(0f, TextFx.letterSpacingPx(108f, 0f), 1e-4f)
    }

    @Test
    fun `visibility window is end-exclusive`() {
        assertTrue(TextFx.isVisibleAt(100L, 100L, 500L))   // start inclusive
        assertFalse(TextFx.isVisibleAt(600L, 100L, 500L))  // end exclusive
        assertFalse(TextFx.isVisibleAt(50L, 100L, 500L))   // before start
    }

    private fun abs(value: Float): Float = if (value < 0f) -value else value
}
