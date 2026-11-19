package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Cream".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object CreamFilter {
    val grade = FilterGrade(
        redGain = 1.053f,
        greenGain = 1.077f,
        blueGain = 1.078f,
        brightness = 0.065f,
        contrast = -0.127f,
        saturation = 0.646f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
