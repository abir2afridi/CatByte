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
        val speed = clip.speed.coerceIn(0.1f, 32f)
        // trimEndMs == 0 is the "until the end" sentinel: the selected source
        // window ends where durationMs (output time) says it does, mapped back
        // through speed so a sped-up clip cannot lose half its source.
        val defaultEnd = (trimStart + (clip.durationMs * speed).roundToLong())
            .coerceAtMost(original)
        val trimEnd = if (clip.trimEndMs > trimStart) {
            clip.trimEndMs.coerceIn(trimStart + 1L, original)
        } else {
            defaultEnd.coerceAtLeast(trimStart + 1L)
        }

        return when (edge) {
            Edge.START -> {
                // Dragging right (outputDeltaMs > 0) shortens clip (trim inward).
                // Dragging left (outputDeltaMs < 0) lengthens clip (trim outward, brings back hidden footage).
                val sourceDeltaMs = (snappedDelta * speed).roundToLong()
                val maxStart = (trimEnd - MIN_DURATION_MS).coerceAtLeast(0L)
                val newStart = (trimStart + sourceDeltaMs).coerceIn(0L, maxStart)

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
                // Dragging right (outputDeltaMs > 0) lengthens clip (trim outward, brings back hidden footage).
                // Dragging left (outputDeltaMs < 0) shortens clip (trim inward).
                val sourceDeltaMs = (snappedDelta * speed).roundToLong()
                val minEnd = (trimStart + MIN_DURATION_MS).coerceAtMost(original)
                val newEnd = (trimEnd + sourceDeltaMs).coerceIn(minEnd, original)

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
