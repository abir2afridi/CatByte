package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Dark Crimson".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object DarkCrimsonFilter {
    val grade = FilterGrade(
        redGain = 0.765f,
        greenGain = 0.782f,
        blueGain = 0.981f,
        brightness = -0.099f,
        contrast = 0.3f,
        saturation = 0.67f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
