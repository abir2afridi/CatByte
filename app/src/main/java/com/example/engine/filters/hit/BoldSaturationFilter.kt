package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Bold Saturation".
 * Look family: vivid neon. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object BoldSaturationFilter {
    val grade = FilterGrade(
        redGain = 1.181f,
        greenGain = 1.0f,
        blueGain = 1.26f,
        brightness = 0.033f,
        contrast = 0.307f,
        saturation = 1.623f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
