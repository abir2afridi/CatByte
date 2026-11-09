package com.example.engine

import com.example.model.Keyframe
import com.example.model.KeyframeProperty

/**
 * Motion Tracking engine for anchoring stickers and text overlays to moving subjects across keyframes.
 */
object MotionTracker {

    data class TrackPoint(val timeMs: Long, val x: Float, val y: Float)

    /**
     * Generates position X and Y keyframes from a series of tracked coordinate points.
     */
    fun generateKeyframes(points: List<TrackPoint>): List<Keyframe> {
        val keyframes = mutableListOf<Keyframe>()
        for (pt in points) {
            keyframes.add(
                Keyframe(
                    timestampMs = pt.timeMs,
                    property = KeyframeProperty.POSITION_X,
                    value = pt.x
                )
            )
            keyframes.add(
                Keyframe(
                    timestampMs = pt.timeMs,
                    property = KeyframeProperty.POSITION_Y,
                    value = pt.y
                )
            )
        }
        return keyframes
    }
}
