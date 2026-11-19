package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Violet Rush".
 * Look family: pink-violet tint. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object VioletRushFilter {
    val grade = FilterGrade(
        redGain = 1.188f,
        greenGain = 0.934f,
        blueGain = 1.207f,
        brightness = 0.132f,
        contrast = 0.176f,
        saturation = 1.198f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
