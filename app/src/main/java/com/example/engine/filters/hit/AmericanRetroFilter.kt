package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "American Retro".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object AmericanRetroFilter {
    val grade = FilterGrade(
        redGain = 1.163f,
        greenGain = 1.052f,
        blueGain = 0.896f,
        brightness = 0.118f,
        contrast = -0.082f,
        saturation = 0.639f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
