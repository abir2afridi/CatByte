package com.example.model

// Phase 2 Enums & Models
enum class SpeedCurvePreset(val displayName: String) {
    STANDARD("Standard (Constant)"),
    MONTAGE("Montage Wave"),
    HERO_MOMENT("Hero Moment"),
    BULLET_TIME("Bullet Time"),
    JUMP_CUT("Flash Fast"),
    CUSTOM("Custom Bezier")
}

data class BezierPoint(
    val x: Float, // 0.0f to 1.0f (Timeline Ratio)
    val speedMultiplier: Float // 0.1f to 10.0f
)

data class SpeedCurve(
    val preset: SpeedCurvePreset = SpeedCurvePreset.STANDARD,
    val points: List<BezierPoint> = listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
)