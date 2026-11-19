package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Wilderness Night".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object WildernessNightFilter {
    val grade = FilterGrade(
        redGain = 0.841f,
        greenGain = 0.821f,
        blueGain = 1.0f,
        brightness = -0.086f,
        contrast = 0.306f,
        saturation = 0.673f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
