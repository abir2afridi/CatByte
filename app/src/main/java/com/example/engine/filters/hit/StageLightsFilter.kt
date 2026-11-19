package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Stage Lights".
 * Look family: vivid neon. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object StageLightsFilter {
    val grade = FilterGrade(
        redGain = 1.195f,
        greenGain = 1.068f,
        blueGain = 1.234f,
        brightness = 0.016f,
        contrast = 0.298f,
        saturation = 1.619f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
