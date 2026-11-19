package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "LA Classic".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object LaClassicFilter {
    val grade = FilterGrade(
        redGain = 1.107f,
        greenGain = 0.964f,
        blueGain = 0.792f,
        brightness = 0.048f,
        contrast = -0.116f,
        saturation = 0.702f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
