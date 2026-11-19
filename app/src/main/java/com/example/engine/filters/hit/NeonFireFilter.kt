package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Neon Fire".
 * Look family: vivid neon. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object NeonFireFilter {
    val grade = FilterGrade(
        redGain = 1.166f,
        greenGain = 1.053f,
        blueGain = 1.287f,
        brightness = 0.131f,
        contrast = 0.275f,
        saturation = 1.688f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
