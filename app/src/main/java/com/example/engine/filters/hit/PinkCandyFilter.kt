package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Pink Candy".
 * Look family: pink-violet tint. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PinkCandyFilter {
    val grade = FilterGrade(
        redGain = 1.198f,
        greenGain = 0.879f,
        blueGain = 1.179f,
        brightness = 0.113f,
        contrast = 0.167f,
        saturation = 1.274f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
