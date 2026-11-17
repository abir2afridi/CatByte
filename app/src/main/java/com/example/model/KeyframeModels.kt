package com.example.model

import java.util.UUID

enum class KeyframeProperty(val displayName: String) {
    POSITION_X("Position X"),
    POSITION_Y("Position Y"),
    SCALE("Scale"),
    ROTATION("Rotation"),
    OPACITY("Opacity"),
    VOLUME("Volume")
}

enum class EasingCurve(val displayName: String) {
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    EASE_IN_OUT("Ease In-Out"),
    BOUNCE("Bounce"),
    CUSTOM_BEZIER("Custom Bezier")
}

// Phase 1 Data Models
data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long,
    val property: KeyframeProperty,
    val value: Float,
    val easing: EasingCurve = EasingCurve.EASE_IN_OUT
)