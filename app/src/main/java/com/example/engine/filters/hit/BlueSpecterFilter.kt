package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Blue Specter".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object BlueSpecterFilter {
    val grade = FilterGrade(
        redGain = 0.834f,
        greenGain = 0.877f,
        blueGain = 0.968f,
        brightness = -0.188f,
        contrast = 0.256f,
        saturation = 0.728f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
