package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Burgundy".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object BurgundyFilter {
    val grade = FilterGrade(
        redGain = 0.985f,
        greenGain = 0.962f,
        blueGain = 1.041f,
        brightness = 0.071f,
        contrast = 0.181f,
        saturation = 1.11f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
