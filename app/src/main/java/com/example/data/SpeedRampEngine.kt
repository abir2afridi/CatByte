package com.example.data

import com.example.model.BezierPoint
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset

object SpeedRampEngine {

    /** Returns standard speed curve preset points. */
    fun getPresetCurve(preset: SpeedCurvePreset): SpeedCurve {
        val points = when (preset) {
            SpeedCurvePreset.STANDARD -> listOf(
                BezierPoint(0.0f, 1.0f),
                BezierPoint(1.0f, 1.0f)
            )
            SpeedCurvePreset.MONTAGE -> listOf(
                BezierPoint(0.0f, 1.0f),
                BezierPoint(0.2f, 3.0f),
                BezierPoint(0.5f, 0.4f),
                BezierPoint(0.8f, 2.5f),
                BezierPoint(1.0f, 1.0f)
            )
            SpeedCurvePreset.HERO_MOMENT -> listOf(
                BezierPoint(0.0f, 1.0f),
                BezierPoint(0.3f, 0.3f),
                BezierPoint(0.7f, 4.0f),
                BezierPoint(1.0f, 1.0f)
            )
            SpeedCurvePreset.BULLET_TIME -> listOf(
                BezierPoint(0.0f, 1.0f),
                BezierPoint(0.15f, 5.0f),
                BezierPoint(0.5f, 0.2f),
                BezierPoint(0.85f, 5.0f),
                BezierPoint(1.0f, 1.0f)
            )
            SpeedCurvePreset.JUMP_CUT -> listOf(
                BezierPoint(0.0f, 0.5f),
                BezierPoint(0.4f, 6.0f),
                BezierPoint(0.6f, 6.0f),
                BezierPoint(1.0f, 0.5f)
            )
            SpeedCurvePreset.CUSTOM -> listOf(
                BezierPoint(0.0f, 1.0f),
                BezierPoint(0.5f, 2.0f),
                BezierPoint(1.0f, 1.0f)
            )
        }
        return SpeedCurve(preset = preset, points = points)
    }

    /**
     * Calculates the speed multiplier at progress [progressRatio] (0f to 1f)
     * along the clip.
     */
    fun getSpeedMultiplierAt(curve: SpeedCurve, progressRatio: Float): Float {
        val sorted = curve.points.sortedBy { it.x }
        if (sorted.isEmpty()) return 1.0f

        val ratio = progressRatio.coerceIn(0f, 1f)
        if (ratio <= sorted.first().x) return sorted.first().speedMultiplier
        if (ratio >= sorted.last().x) return sorted.last().speedMultiplier

        val nextIdx = sorted.indexOfFirst { it.x >= ratio }
        if (nextIdx <= 0) return sorted.first().speedMultiplier

        val p1 = sorted[nextIdx - 1]
        val p2 = sorted[nextIdx]

        val t = (ratio - p1.x) / (p2.x - p1.x).coerceAtLeast(0.001f)
        return p1.speedMultiplier + t * (p2.speedMultiplier - p1.speedMultiplier)
    }
}
