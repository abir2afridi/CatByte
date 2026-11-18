package com.example.engine

import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import kotlin.math.abs

/**
 * Slow push-in ("Ken Burns") for stills and video clips.
 *
 * The move is stored as ordinary SCALE keyframes, so the preview compositor
 * and the export renderer evaluate the exact same motion - there is no
 * separate animation path to drift apart. The zoom starts at the clip's own
 * scale and never dips below it, so no black edge can enter the frame.
 */
object KenBurns {

    const val START_SCALE = 1f
    const val END_SCALE = 1.12f

    /** Below this the zoom would be a jarring jump instead of a slow move. */
    const val MIN_DURATION_MS = 500L

    /** The two keyframes that make the move for a clip of [durationMs]. */
    fun keyframes(durationMs: Long, startScale: Float = START_SCALE): List<Keyframe> {
        // Start from the clip's own framing so a clip the user already scaled
        // does not jump back to 1x when the slow zoom is applied.
        val start = startScale.coerceAtLeast(0.01f)
        return listOf(
            Keyframe(
                timestampMs = 0L,
                property = KeyframeProperty.SCALE,
                value = start,
                easing = EasingCurve.EASE_IN_OUT
            ),
            Keyframe(
                timestampMs = durationMs.coerceAtLeast(0L),
                property = KeyframeProperty.SCALE,
                value = endScale(start),
                easing = EasingCurve.EASE_IN_OUT
            )
        )
    }

    /** The scale the move ends on. */
    fun endScale(startScale: Float): Float =
        startScale.coerceAtLeast(0.01f) * (END_SCALE / START_SCALE)

    /**
     * True when [keyframes] hold exactly the pair [keyframes] creates, which
     * is what the toggle sees as "already applied".
     */
    fun isApplied(
        keyframes: List<Keyframe>,
        durationMs: Long,
        startScale: Float = START_SCALE
    ): Boolean {
        val scale = keyframes
            .filter { it.property == KeyframeProperty.SCALE }
            .sortedBy { it.timestampMs }
        if (scale.size != 2) return false
        val start = scale[0]
        val end = scale[1]
        return start.timestampMs == 0L &&
            end.timestampMs == durationMs &&
            abs(start.value - startScale.coerceAtLeast(0.01f)) < 0.0001f &&
            abs(end.value - endScale(startScale)) < 0.0001f
    }

    /**
     * True when scale/position keyframes already drive this clip. Adding the
     * zoom would fight them, so the caller explains this instead of stacking
     * two animations on one property.
     */
    fun hasConflictingKeyframes(keyframes: List<Keyframe>): Boolean = keyframes.any {
        it.property == KeyframeProperty.SCALE ||
            it.property == KeyframeProperty.POSITION_X ||
            it.property == KeyframeProperty.POSITION_Y
    }
}
