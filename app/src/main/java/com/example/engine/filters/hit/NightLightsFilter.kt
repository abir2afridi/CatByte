package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Night Lights".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object NightLightsFilter {
    val grade = FilterGrade(
        redGain = 0.868f,
        greenGain = 0.894f,
        blueGain = 1.037f,
        brightness = -0.062f,
        contrast = 0.319f,
        saturation = 0.76f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
