package com.example.engine

import kotlin.math.abs

/**
 * Stereo balance maths for the audio mixer.
 *
 * The balance is a value between -1 (hard left) and +1 (hard right). The side
 * the track moves away from fades out, the other side keeps its level, so a
 * balance can never make a track louder or clip the mix.
 */
object StereoPan {

    const val MIN_PAN = -1f
    const val MAX_PAN = 1f

    /** The per-channel gains of a balance value. */
    data class Gains(val left: Float, val right: Float)

    /** Clamps a stored or user-supplied balance into the valid range. */
    fun clamp(pan: Float): Float =
        if (pan.isFinite()) pan.coerceIn(MIN_PAN, MAX_PAN) else 0f

    /**
     * Gains for a balance value: panning right fades the left channel out and
     * panning left fades the right channel out.
     */
    fun gains(pan: Float): Gains {
        val value = clamp(pan)
        return Gains(
            left = if (value > 0f) 1f - value else 1f,
            right = if (value < 0f) 1f + value else 1f
        )
    }

    /** Short position label for the mixer: "C", "L 40", "R 25". */
    fun label(pan: Float): String {
        val value = clamp(pan)
        val percent = abs(value * 100f).toInt()
        return when {
            percent == 0 -> "C"
            value < 0f -> "L $percent"
            else -> "R $percent"
        }
    }
}
