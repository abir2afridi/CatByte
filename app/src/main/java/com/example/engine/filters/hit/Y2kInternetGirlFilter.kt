package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Y2k-internet Girl".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object Y2kInternetGirlFilter {
    val grade = FilterGrade(
        redGain = 1.132f,
        greenGain = 0.976f,
        blueGain = 0.858f,
        brightness = 0.012f,
        contrast = -0.134f,
        saturation = 0.613f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
