package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Cold Thriller".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object ColdThrillerFilter {
    val grade = FilterGrade(
        redGain = 0.837f,
        greenGain = 0.878f,
        blueGain = 1.029f,
        brightness = -0.067f,
        contrast = 0.397f,
        saturation = 0.799f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
