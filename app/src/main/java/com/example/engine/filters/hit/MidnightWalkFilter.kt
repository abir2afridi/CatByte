package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Midnight Walk".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object MidnightWalkFilter {
    val grade = FilterGrade(
        redGain = 0.764f,
        greenGain = 0.842f,
        blueGain = 0.951f,
        brightness = -0.119f,
        contrast = 0.37f,
        saturation = 0.786f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
