package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Moody Vibe".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object MoodyVibeFilter {
    val grade = FilterGrade(
        redGain = 1.044f,
        greenGain = 0.992f,
        blueGain = 1.056f,
        brightness = 0.081f,
        contrast = 0.186f,
        saturation = 1.113f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
