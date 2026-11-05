package com.example.engine

import com.example.data.ClipTiming
import com.example.model.ClipAnimation
import com.example.model.MediaClip
import com.example.model.TransitionType

/**
 * Frame-accurate clip splitting.
 *
 * The editor stores the *presentation* length in [MediaClip.durationMs] while
 * the source is trimmed and possibly sped up. A split therefore has to convert
 * the timeline offset back into a source offset before moving the trim points;
 * otherwise the two halves would overlap or skip frames.
 */
object ClipSplit {

    /** A split this close to either edge would create an unusable sliver. */
    const val MIN_SPLIT_MS = 100L

    data class Result(val first: MediaClip, val second: MediaClip)

    /**
     * Splits [clip] at [outputOffsetMs] (a timeline-local position).
     *
     * Returns null when the offset is outside the clip or too close to an edge.
     * Ids are minted through [idFactory] so callers stay deterministic in tests.
     */
    fun split(
        clip: MediaClip,
        outputOffsetMs: Long,
        idFactory: () -> String
    ): Result? {
        if (outputOffsetMs < MIN_SPLIT_MS) return null
        val secondDuration = clip.durationMs - outputOffsetMs
        if (secondDuration < MIN_SPLIT_MS) return null

        val sourceLength = ClipTiming.sourceLengthMs(clip)
        val sourceOffset = ClipTiming.sourceOffsetForOutputMs(clip, outputOffsetMs)
        if (sourceOffset <= 0L || sourceOffset >= sourceLength) return null

        val trimStart = clip.trimStartMs.coerceIn(0L, (clip.originalDurationMs - 1L).coerceAtLeast(0L))
        val trimEnd = if (clip.trimEndMs > trimStart) {
            clip.trimEndMs.coerceIn(trimStart + 1L, clip.originalDurationMs.coerceAtLeast(trimStart + 1L))
        } else {
            clip.originalDurationMs.coerceAtLeast(trimStart + 1L)
        }
        val splitSource = (trimStart + sourceOffset).coerceIn(trimStart + 1L, trimEnd - 1L)

        val firstKeyframes = clip.keyframes.filter { it.timestampMs < outputOffsetMs }
        val secondKeyframes = clip.keyframes
            .filter { it.timestampMs >= outputOffsetMs }
            .map { it.copy(timestampMs = (it.timestampMs - outputOffsetMs).coerceAtLeast(0L)) }

        val first = clip.copy(
            id = idFactory(),
            durationMs = outputOffsetMs,
            trimStartMs = trimStart,
            trimEndMs = splitSource,
            keyframes = firstKeyframes,
            // The transition belonged to the original clip's end, which is now
            // the middle of the edit. Keep it on the second half only.
            transition = TransitionType.NONE
        )
        val second = clip.copy(
            id = idFactory(),
            durationMs = secondDuration,
            trimStartMs = splitSource,
            trimEndMs = trimEnd,
            keyframes = secondKeyframes,
            // The entrance animation belongs to the start of the original clip.
            animationIn = ClipAnimation.NONE
        )

        return Result(first = first, second = second)
    }
}
