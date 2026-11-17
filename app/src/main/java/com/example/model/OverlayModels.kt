package com.example.model

import java.util.UUID

data class StickerOverlay(
    val id: String,
    val symbol: String,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 3000L,
    val posX: Float = 0.5f,
    val posY: Float = 0.3f,
    val scale: Float = 1.0f,
    val keyframes: List<Keyframe> = emptyList()
)

/**
 * A freehand drawing stroke ("Text → Draw"). Points are normalised 0..1 canvas
 * coordinates so the stroke scales with the canvas in preview and export.
 */
data class StrokeOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    /** Flat x,y pairs in 0..1 canvas space. */
    val points: List<Float> = emptyList(),
    val colorArgb: Long = 0xFFFF3B30,
    /** Stroke width as a fraction of the canvas width. */
    val widthFraction: Float = 0.010f,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 3000L
) {
    val isEmpty: Boolean get() = points.size < 4
}

data class TimelineEffect(
    val id: String,
    val name: String,
    val effectType: EffectType,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 2000L,
    /** User multiplier on the preset's intensity (0.2 .. 2.0). */
    val intensityScale: Float = 1f
)

/** A timeline marker: labelled magnet and jump target. */
data class TimelineMarker(
    val id: String = UUID.randomUUID().toString(),
    val atMs: Long = 0L,
    val label: String = "",
    val colorArgb: Long = 0xFFFFC94A
)