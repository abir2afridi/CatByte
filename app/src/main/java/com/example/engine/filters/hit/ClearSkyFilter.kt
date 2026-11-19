package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Clear Sky".
 * Look family: cool cyan. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object ClearSkyFilter {
    val grade = FilterGrade(
        redGain = 0.85f,
        greenGain = 1.035f,
        blueGain = 1.298f,
        brightness = 0.089f,
        contrast = 0.139f,
        saturation = 1.06f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
