package com.example.engine

import com.example.data.ClipTiming
import com.example.data.ClipTiming.hasSpeedCurve
import com.example.model.MediaClip
import kotlin.math.roundToLong

/**
 * Inline trim handle maths.
 *
 * The timeline draws handles on the selected clip; this object decides the new
 * source trim points. Keeping it in the engine means the sheet trim and the
 * timeline handles cannot disagree, and the rounding runs in the same unit tests
 * as the rest of the timing code.
 *
 * A velocity curve changes the source↔timeline mapping over the whole clip, so
 * inline handles refuse to move those clips: the Edit Clip sheet owns them.
 */
object ClipTrimHandle {

    /** Clips shorter than this are useless; matches the Edit Clip sheet. */
    const val MIN_DURATION_MS = 300L

    enum class Edge { START, END }

    data class Result(
        val trimStartMs: Long,
        val trimEndMs: Long,
        val durationMs: Long
    )

    /**
     * New trims for dragging [edge] by [outputDeltaMs] on the timeline.
     *
     * Returns null when the edit is a no-op, unsupported (velocity curve) or
     * would break the minimum duration. [fps] quantizes the delta to whole
     * frames so a handle never lands between two images.
     */
    fun trim(
        clip: MediaClip,
        edge: Edge,
        outputDeltaMs: Long,
        fps: Int
    ): Result? {
        if (outputDeltaMs == 0L || fps <= 0) return null
        if (hasSpeedCurve(clip)) return null

        val rational = Rational.fromLegacy(fps)
        val frameMs = rational.frameDurationUs().toMs()
        val snappedDelta = quantize(outputDeltaMs, frameMs)
        if (snappedDelta == 0L) return null

        val original = clip.originalDurationMs.coerceAtLeast(1L)
        val trimStart = clip.trimStartMs.coerceIn(0L, (original - 1L).coerceAtLeast(0L))
        val trimEnd = if (clip.trimEndMs > trimStart) {
            clip.trimEndMs.coerceIn(trimStart + 1L, original)
        } else {
            original
        }
        val duration = clip.durationMs.coerceAtLeast(1L)
        val speed = clip.speed.coerceIn(0.1f, 32f)

        return when (edge) {
            Edge.START -> {
                // Dragging right shortens the clip: the source start moves in.
                val newDuration = (duration - snappedDelta)
                    .coerceIn(MIN_DURATION_MS, duration)
                if (newDuration == duration && snappedDelta > 0L) return null
                val cutSource = ((duration - newDuration) * speed).roundToLong()
                val newStart = (trimStart + cutSource).coerceIn(0L, (trimEnd - 1L).coerceAtLeast(0L))
                val result = clip.copy(
                    trimStartMs = newStart,
                    trimEndMs = trimEnd,
                    durationMs = 0L
                )
                Result(
                    trimStartMs = newStart,
                    trimEndMs = trimEnd,
                    durationMs = ClipTiming.presentationDurationMs(
                        result.copy(durationMs = 1L)
                    )
                )
            }

            Edge.END -> {
                // Dragging right lengthens the clip up to the real source end.
                val maxSource = (original - trimStart).coerceAtLeast(1L)
                val maxOutput = (maxSource / speed).toLong()
                val newDuration = (duration + snappedDelta).coerceIn(duration, maxOutput)
                val newSource = ((newDuration * speed).roundToLong()).coerceIn(1L, maxSource)
                if (newDuration == duration && snappedDelta > 0L) return null
                val newEnd = trimStart + newSource
                val result = clip.copy(
                    trimStartMs = trimStart,
                    trimEndMs = newEnd,
                    durationMs = 0L
                )
                Result(
                    trimStartMs = trimStart,
                    trimEndMs = newEnd,
                    durationMs = ClipTiming.presentationDurationMs(
                        result.copy(durationMs = 1L)
                    )
                )
            }
        }
    }

    private fun quantize(deltaMs: Long, frameMs: Long): Long {
        if (frameMs <= 0L) return deltaMs
        val frames = (deltaMs.toDouble() / frameMs.toDouble()).roundToLong()
        return frames * frameMs
    }
}
