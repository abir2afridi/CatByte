package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Lavender Ray".
 * Look family: pink-violet tint. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object LavenderRayFilter {
    val grade = FilterGrade(
        redGain = 1.219f,
        greenGain = 0.95f,
        blueGain = 1.215f,
        brightness = 0.137f,
        contrast = 0.098f,
        saturation = 1.159f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
