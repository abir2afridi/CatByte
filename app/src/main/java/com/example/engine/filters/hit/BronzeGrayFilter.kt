package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Bronze Gray".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object BronzeGrayFilter {
    val grade = FilterGrade(
        redGain = 1.169f,
        greenGain = 1.045f,
        blueGain = 0.793f,
        brightness = 0.132f,
        contrast = 0.131f,
        saturation = 1.055f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
