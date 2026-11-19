package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Momento".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object MomentoFilter {
    val grade = FilterGrade(
        redGain = 1.137f,
        greenGain = 0.979f,
        blueGain = 0.799f,
        brightness = 0.053f,
        contrast = -0.114f,
        saturation = 0.623f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
