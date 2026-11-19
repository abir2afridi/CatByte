package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Y2K Vogue".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object Y2kVogueFilter {
    val grade = FilterGrade(
        redGain = 1.089f,
        greenGain = 1.015f,
        blueGain = 0.817f,
        brightness = -0.016f,
        contrast = -0.148f,
        saturation = 0.606f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
