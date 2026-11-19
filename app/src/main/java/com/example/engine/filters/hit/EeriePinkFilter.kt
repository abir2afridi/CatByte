package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Eerie Pink".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object EeriePinkFilter {
    val grade = FilterGrade(
        redGain = 0.79f,
        greenGain = 0.795f,
        blueGain = 0.987f,
        brightness = -0.095f,
        contrast = 0.382f,
        saturation = 0.711f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
