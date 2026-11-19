package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Digicam Flash".
 * Look family: vivid neon. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object DigicamFlashFilter {
    val grade = FilterGrade(
        redGain = 1.194f,
        greenGain = 1.007f,
        blueGain = 1.264f,
        brightness = 0.116f,
        contrast = 0.348f,
        saturation = 1.644f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
