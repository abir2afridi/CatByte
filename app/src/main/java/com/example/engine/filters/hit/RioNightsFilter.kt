package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Rio Nights".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object RioNightsFilter {
    val grade = FilterGrade(
        redGain = 0.859f,
        greenGain = 0.829f,
        blueGain = 1.005f,
        brightness = -0.083f,
        contrast = 0.308f,
        saturation = 0.674f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
