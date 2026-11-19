package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Dark Opera".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object DarkOperaFilter {
    val grade = FilterGrade(
        redGain = 0.823f,
        greenGain = 0.811f,
        blueGain = 0.996f,
        brightness = -0.17f,
        contrast = 0.345f,
        saturation = 0.693f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
