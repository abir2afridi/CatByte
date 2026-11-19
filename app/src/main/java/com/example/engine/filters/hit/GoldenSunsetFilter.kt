package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Golden Sunset".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object GoldenSunsetFilter {
    val grade = FilterGrade(
        redGain = 1.169f,
        greenGain = 1.045f,
        blueGain = 0.732f,
        brightness = 0.092f,
        contrast = 0.111f,
        saturation = 1.125f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
