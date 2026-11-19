package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Stranger Day".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object StrangerDayFilter {
    val grade = FilterGrade(
        redGain = 0.988f,
        greenGain = 0.964f,
        blueGain = 1.042f,
        brightness = -0.009f,
        contrast = 0.061f,
        saturation = 1.13f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
