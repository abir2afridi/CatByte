package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Silver Age".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SilverAgeFilter {
    val grade = FilterGrade(
        redGain = 1.092f,
        greenGain = 1.036f,
        blueGain = 1.058f,
        brightness = 0.132f,
        contrast = -0.014f,
        saturation = 0.783f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
