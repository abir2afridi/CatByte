package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Sleepless City".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SleeplessCityFilter {
    val grade = FilterGrade(
        redGain = 1.065f,
        greenGain = 1.063f,
        blueGain = 1.031f,
        brightness = -0.016f,
        contrast = 0.137f,
        saturation = 1.088f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
