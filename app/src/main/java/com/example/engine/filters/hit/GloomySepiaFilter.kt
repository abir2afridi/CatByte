package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Gloomy Sepia".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object GloomySepiaFilter {
    val grade = FilterGrade(
        redGain = 0.852f,
        greenGain = 0.886f,
        blueGain = 1.033f,
        brightness = -0.145f,
        contrast = 0.278f,
        saturation = 0.659f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
