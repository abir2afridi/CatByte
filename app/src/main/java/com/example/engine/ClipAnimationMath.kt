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
 * composition and the exported file animate identically. Fade-in is the one
 * preset that needs per-frame alpha and therefore waits for the GL compositor.
 */
object ClipAnimationMath {

    const val DURATION_MS = 600L

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
            // Alpha ramp: no matrix equivalent, so the graph leaves the clip
            // untouched instead of faking a fade with scale.
            ClipAnimation.FADE_IN -> AnimationTransform()
        }
    }

    /** True when the preset changes the transform before the first frame. */
    fun hasMotion(animation: ClipAnimation): Boolean = when (animation) {
        ClipAnimation.NONE, ClipAnimation.FADE_IN -> false
        else -> true
    }
}
