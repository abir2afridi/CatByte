package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Light Boost".
 * Look family: vivid neon. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object LightBoostFilter {
    val grade = FilterGrade(
        redGain = 1.129f,
        greenGain = 1.034f,
        blueGain = 1.277f,
        brightness = 0.045f,
        contrast = 0.312f,
        saturation = 1.706f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
