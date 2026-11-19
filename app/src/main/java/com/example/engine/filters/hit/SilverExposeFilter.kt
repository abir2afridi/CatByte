package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Silver Expose".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SilverExposeFilter {
    val grade = FilterGrade(
        redGain = 1.024f,
        greenGain = 1.002f,
        blueGain = 1.101f,
        brightness = 0.081f,
        contrast = -0.12f,
        saturation = 0.73f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
