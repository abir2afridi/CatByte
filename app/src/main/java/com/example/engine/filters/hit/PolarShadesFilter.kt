package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Polar Shades".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PolarShadesFilter {
    val grade = FilterGrade(
        redGain = 1.045f,
        greenGain = 1.053f,
        blueGain = 1.086f,
        brightness = 0.101f,
        contrast = 0.196f,
        saturation = 1.198f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
