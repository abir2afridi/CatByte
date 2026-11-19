package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Retro Studio".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object RetroStudioFilter {
    val grade = FilterGrade(
        redGain = 1.167f,
        greenGain = 1.054f,
        blueGain = 0.837f,
        brightness = -0.002f,
        contrast = -0.141f,
        saturation = 0.69f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
