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

data class TimelineEffect(
    val id: String,
    val name: String,
    val effectType: EffectType,
    val startTimelineMs: Long = 0L,
    val durationMs: Long = 2000L
)

/** A timeline marker: labelled magnet and jump target. */
data class TimelineMarker(
    val id: String = UUID.randomUUID().toString(),
    val atMs: Long = 0L,
    val label: String = "",
    val colorArgb: Long = 0xFFFFC94A
)