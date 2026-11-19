package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Shimmering B&W".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object ShimmeringBandwFilter {
    val grade = FilterGrade(
        redGain = 1.039f,
        greenGain = 1.05f,
        blueGain = 1.085f,
        brightness = 0.02f,
        contrast = 0.074f,
        saturation = 1.057f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
