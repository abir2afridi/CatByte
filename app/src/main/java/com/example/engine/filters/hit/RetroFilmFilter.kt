package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Retro Film".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object RetroFilmFilter {
    val grade = FilterGrade(
        redGain = 1.116f,
        greenGain = 0.968f,
        blueGain = 0.854f,
        brightness = 0.009f,
        contrast = -0.136f,
        saturation = 0.692f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
