package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Cool Black".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object CoolBlackFilter {
    val grade = FilterGrade(
        redGain = 0.767f,
        greenGain = 0.844f,
        blueGain = 1.012f,
        brightness = -0.078f,
        contrast = 0.311f,
        saturation = 0.755f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
