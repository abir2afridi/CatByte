package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Spring Scene".
 * Look family: green cast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SpringSceneFilter {
    val grade = FilterGrade(
        redGain = 0.851f,
        greenGain = 1.216f,
        blueGain = 0.908f,
        brightness = 0.012f,
        contrast = 0.196f,
        saturation = 1.028f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
