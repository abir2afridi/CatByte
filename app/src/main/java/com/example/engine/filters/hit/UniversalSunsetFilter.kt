package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Universal Sunset".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object UniversalSunsetFilter {
    val grade = FilterGrade(
        redGain = 1.214f,
        greenGain = 1.067f,
        blueGain = 0.743f,
        brightness = 0.019f,
        contrast = 0.074f,
        saturation = 1.107f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
