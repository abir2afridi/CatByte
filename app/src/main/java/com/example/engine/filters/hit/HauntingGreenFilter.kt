package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Haunting Green".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object HauntingGreenFilter {
    val grade = FilterGrade(
        redGain = 0.789f,
        greenGain = 0.854f,
        blueGain = 0.957f,
        brightness = -0.196f,
        contrast = 0.252f,
        saturation = 0.646f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
