package com.example.data

import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
import kotlin.math.pow

object KeyframeInterpolator {

    /**
     * Evaluates a property value at [timestampMs] for a list of [keyframes].
     * If no keyframes are defined for [property], returns [defaultValue].
     */
    fun interpolate(
        keyframes: List<Keyframe>,
        property: KeyframeProperty,
        timestampMs: Long,
        defaultValue: Float
    ): Float {
        val filtered = keyframes.filter { it.property == property }.sortedBy { it.timestampMs }
        if (filtered.isEmpty()) return defaultValue

        // Before first keyframe
        if (timestampMs <= filtered.first().timestampMs) {
            return filtered.first().value
        }
        // After last keyframe
        if (timestampMs >= filtered.last().timestampMs) {
            return filtered.last().value
        }

        // Find surrounding keyframes
        val nextIndex = filtered.indexOfFirst { it.timestampMs >= timestampMs }
        if (nextIndex <= 0) return filtered.first().value

        val prev = filtered[nextIndex - 1]
        val next = filtered[nextIndex]

        val timeDelta = (next.timestampMs - prev.timestampMs).coerceAtLeast(1L)
        val progress = ((timestampMs - prev.timestampMs).toFloat() / timeDelta).coerceIn(0f, 1f)

        val easedProgress = applyEasing(progress, next.easing)
        return prev.value + (next.value - prev.value) * easedProgress
    }

    /** Returns interpolated scale for [clip] at [timestampMs]. */
    fun getScale(clip: MediaClip, timestampMs: Long): Float {
        return interpolate(clip.keyframes, KeyframeProperty.SCALE, timestampMs, clip.scale)
    }

    /** Returns interpolated Position X for [clip] at [timestampMs]. */
    fun getPosX(clip: MediaClip, timestampMs: Long): Float {
        return interpolate(clip.keyframes, KeyframeProperty.POSITION_X, timestampMs, clip.posX)
    }

    /** Returns interpolated Position Y for [clip] at [timestampMs]. */
    fun getPosY(clip: MediaClip, timestampMs: Long): Float {
        return interpolate(clip.keyframes, KeyframeProperty.POSITION_Y, timestampMs, clip.posY)
    }

    /** Returns interpolated Rotation for [clip] at [timestampMs]. */
    fun getRotation(clip: MediaClip, timestampMs: Long): Float {
        return interpolate(clip.keyframes, KeyframeProperty.ROTATION, timestampMs, clip.rotation)
    }

    /** Returns interpolated Opacity for [clip] at [timestampMs]. */
    fun getOpacity(clip: MediaClip, timestampMs: Long): Float {
        return interpolate(clip.keyframes, KeyframeProperty.OPACITY, timestampMs, clip.opacity)
    }

    private fun applyEasing(fraction: Float, curve: EasingCurve): Float {
        return when (curve) {
            EasingCurve.LINEAR -> fraction
            EasingCurve.EASE_IN -> fraction * fraction
            EasingCurve.EASE_OUT -> fraction * (2f - fraction)
            EasingCurve.EASE_IN_OUT -> if (fraction < 0.5f) 2f * fraction * fraction else -1f + (4f - 2f * fraction) * fraction
            EasingCurve.BOUNCE -> bounceEasing(fraction)
        }
    }

    private fun bounceEasing(fraction: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        var x = fraction
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                x -= 1.5f / d1
                n1 * x * x + 0.75f
            }
            x < 2.5f / d1 -> {
                x -= 2.25f / d1
                n1 * x * x + 0.9375f
            }
            else -> {
                x -= 2.625f / d1
                n1 * x * x + 0.984375f
            }
        }
    }
}
