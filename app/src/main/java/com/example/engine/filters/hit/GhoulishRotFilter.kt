package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Ghoulish Rot".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object GhoulishRotFilter {
    val grade = FilterGrade(
        redGain = 0.84f,
        greenGain = 0.88f,
        blueGain = 0.97f,
        brightness = -0.106f,
        contrast = 0.377f,
        saturation = 0.789f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
