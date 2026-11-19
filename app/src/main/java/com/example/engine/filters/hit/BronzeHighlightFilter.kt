package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Bronze Highlight".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object BronzeHighlightFilter {
    val grade = FilterGrade(
        redGain = 1.256f,
        greenGain = 1.088f,
        blueGain = 0.754f,
        brightness = 0.026f,
        contrast = 0.158f,
        saturation = 1.069f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
