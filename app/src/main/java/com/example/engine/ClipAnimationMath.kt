package com.example.engine

import com.example.model.ClipAnimation
import kotlin.math.PI
import kotlin.math.cos

/** Normalised transform offset contributed by a clip entry animation. */
data class AnimationTransform(
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val translateX: Float = 0f,
    val translateY: Float = 0f
)

/**
 * Entry-animation curves used by the shared render graph.
 *
 * These feed [com.example.data.ProjectCompositionFactory]'s
 * `ClipTransformEffect`, which Media3 samples per frame, so the preview
 * composition and the exported file animate identically. The "fade in"
 * preset needs a per-frame colour change rather than a matrix, so it is
 * rendered by `ClipFadeInEffect` from the same progress curve.
 */
object ClipAnimationMath {

    /** Default length of an entry animation. */
    const val DURATION_MS = 600L

    /** Shortest animation the render graph accepts. */
    const val MIN_DURATION_MS = 100L

    /** Longest animation the render graph accepts. */
    const val MAX_DURATION_MS = 5_000L

    /** Clamps an animation length into the range the renderer supports. */
    fun sanitizeDuration(durationMs: Long): Long =
        if (durationMs <= 0L) DURATION_MS else durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)

    fun transformAt(
        animation: ClipAnimation,
        timeMs: Long,
        durationMs: Long = DURATION_MS
    ): AnimationTransform {
        if (animation == ClipAnimation.NONE || durationMs <= 0L) return AnimationTransform()
        val progress = (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val remaining = 1f - progress
        return when (animation) {
            ClipAnimation.NONE -> AnimationTransform()
            // 125% -> 100% settle.
            ClipAnimation.ZOOM_IN -> AnimationTransform(scale = 1f + 0.25f * remaining)
            // Enters from off-canvas on the left.
            ClipAnimation.SWIPE_RIGHT -> AnimationTransform(
                translateX = -1.6f * remaining * remaining
            )
            // Damped squash-and-settle rather than a linear zoom.
            ClipAnimation.BOUNCE -> {
                val bounce = remaining * cos(progress * 3f * PI.toFloat())
                AnimationTransform(scale = 1f + 0.18f * bounce)
            }
            ClipAnimation.SPIN -> AnimationTransform(
                rotationDeg = 360f * remaining,
                scale = 1f + 0.10f * remaining
            )
            // A fade is a colour change, not a transform: the matrix stays
            // the identity and ClipFadeInEffect darkens the pixels.
            ClipAnimation.FADE_IN -> AnimationTransform()
        }
    }

    /**
     * The opacity of the entry animation at [timeMs], from 0 to 1.
     *
     * Only the fade-in preset uses it; every other preset returns 1, i.e.
     * "nothing to blend".
     */
    fun fadeProgress(timeMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 1f
        return (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /** True when the preset changes the transform before the first frame. */
    fun hasMotion(animation: ClipAnimation): Boolean = when (animation) {
        ClipAnimation.NONE, ClipAnimation.FADE_IN -> false
        else -> true
    }

    /** True when the preset changes the pixels of the clip. */
    fun needsFade(animation: ClipAnimation): Boolean = animation == ClipAnimation.FADE_IN
}
