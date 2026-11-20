package com.example.engine

import kotlin.math.pow

/**
 * Matrix-expressible colour adjustments beyond the base grade, shared by the
 * preview composition and the export (one maths, one look).
 *
 * Exposure, temperature and tint fold into the same 4x4 colour matrix the
 * filters and the base grade use, so adding them costs nothing at runtime.
 * Vibrance is approximated as a saturation gain - an honest simplification:
 * true vibrance protects already-saturated pixels, which needs per-pixel
 * maths and stays on the GL roadmap.
 */
object ColorAdjustMath {

    /**
     * Exposure in stops (-1..1) as an RGB gain: -1 = half light, 0 = neutral,
     * +1 = double light.
     */
    fun exposureGain(exposure: Float): Float =
        2f.pow(exposure.coerceIn(-1f, 1f))

    /**
     * Temperature (-1 cool..+1 warm) as per-channel gains: warm pushes red up
     * and blue down, cool does the reverse. Green is untouched.
     */
    fun temperatureGains(temperature: Float): Pair<Float, Float> {
        val t = temperature.coerceIn(-1f, 1f)
        return (1f + 0.25f * t) to (1f - 0.25f * t)
    }

    /**
     * Tint (-1 green..+1 magenta) as a green-channel gain: magenta pulls green
     * down, green pushes it up.
     */
    fun tintGain(tint: Float): Float {
        val t = tint.coerceIn(-1f, 1f)
        return 1f - 0.3f * t
    }

    /** Vibrance (-1..1) as a saturation multiplier (matrix approximation). */
    fun vibranceSaturation(vibrance: Float): Float =
        1f + 0.5f * vibrance.coerceIn(-1f, 1f)
}
