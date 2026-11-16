package com.example.engine

import kotlin.math.roundToLong

/**
 * Timing decisions when a clip is relinked to a different source file.
 *
 * Relinking must never read past the end of the replacement file: a clip that
 * used 8 s of a 30 s source cannot keep its length when the user points it at a
 * 3 s file. The trim start is clamped, and the timeline length is shortened to
 * the part that actually exists (speed-aware).
 */
object RelinkMath {

    data class Timing(val trimStartMs: Long, val durationMs: Long)

    /**
     * @param durationMs current timeline length of the clip
     * @param speed clip speed (> 0), 1 = normal
     * @param trimStartMs current source offset
     * @param newSourceMs full length of the replacement source
     */
    fun relinkedTiming(
        durationMs: Long,
        speed: Float,
        trimStartMs: Long,
        newSourceMs: Long
    ): Timing {
        val safeSource = newSourceMs.coerceAtLeast(1L)
        val safeSpeed = if (speed.isFinite() && speed > 0.01f) speed else 1f
        val trim = trimStartMs.coerceIn(0L, safeSource - 1L)
        val usedSourceMs = (durationMs.coerceAtLeast(1L) * safeSpeed).roundToLong()
        val availableSourceMs = (safeSource - trim).coerceAtLeast(1L)
        val keptSourceMs = usedSourceMs.coerceAtMost(availableSourceMs)
        return Timing(
            trimStartMs = trim,
            durationMs = (keptSourceMs / safeSpeed).roundToLong().coerceAtLeast(1L)
        )
    }
}
