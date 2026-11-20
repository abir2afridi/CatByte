package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

/**
 * The extra grade sliders fold into the shared colour matrix. Neutral values
 * must be exact no-ops so an untouched clip is never graded by accident.
 */
class ColorAdjustMathTest {

    private fun assertNear(expected: Float, actual: Float, delta: Float = 1e-5f) {
        assertEquals(expected.toDouble(), actual.toDouble(), delta.toDouble())
    }

    @Test
    fun `exposure doubles and halves light at the extremes`() {
        assertNear(2f, ColorAdjustMath.exposureGain(1f))
        assertNear(0.5f, ColorAdjustMath.exposureGain(-1f))
        assertNear(1f, ColorAdjustMath.exposureGain(0f))
        assertNear(1.4142f, ColorAdjustMath.exposureGain(0.5f), 1e-3f)
        // Out-of-range inputs clamp to the extremes instead of exploding.
        assertNear(2f, ColorAdjustMath.exposureGain(5f))
    }

    @Test
    fun `temperature warms red and cools blue`() {
        val (warmRed, warmBlue) = ColorAdjustMath.temperatureGains(1f)
        assertNear(1.25f, warmRed)
        assertNear(0.75f, warmBlue)
        val (coolRed, coolBlue) = ColorAdjustMath.temperatureGains(-1f)
        assertNear(0.75f, coolRed)
        assertNear(1.25f, coolBlue)
        val (neutralRed, neutralBlue) = ColorAdjustMath.temperatureGains(0f)
        assertNear(1f, neutralRed)
        assertNear(1f, neutralBlue)
    }

    @Test
    fun `tint pulls green down for magenta and up for green`() {
        assertNear(0.7f, ColorAdjustMath.tintGain(1f))
        assertNear(1.3f, ColorAdjustMath.tintGain(-1f))
        assertNear(1f, ColorAdjustMath.tintGain(0f))
    }

    @Test
    fun `vibrance scales saturation`() {
        assertNear(1.5f, ColorAdjustMath.vibranceSaturation(1f))
        assertNear(0.5f, ColorAdjustMath.vibranceSaturation(-1f))
        assertNear(1f, ColorAdjustMath.vibranceSaturation(0f))
    }

    @Test
    fun `neutral grade is an exact identity in the composed matrix`() {
        // The composed effect for a neutral clip must equal the base grade -
        // exposure/temp/tint/vibrance at 0 contribute exactly nothing.
        val exposure = ColorAdjustMath.exposureGain(0f)
        val (tRed, tBlue) = ColorAdjustMath.temperatureGains(0f)
        val tGreen = ColorAdjustMath.tintGain(0f)
        val vibrance = ColorAdjustMath.vibranceSaturation(0f)
        assertEquals(1f, exposure, 1e-6f)
        assertEquals(1f, tRed, 1e-6f)
        assertEquals(1f, tGreen, 1e-6f)
        assertEquals(1f, tBlue, 1e-6f)
        assertEquals(1f, vibrance, 1e-6f)
        val redGain = 1f * 1f * exposure * tRed
        assertNear(1f, redGain)
    }
}
