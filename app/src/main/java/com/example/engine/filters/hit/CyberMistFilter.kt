package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Cyber Mist".
 * Look family: cool cyan. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object CyberMistFilter {
    val grade = FilterGrade(
        redGain = 0.794f,
        greenGain = 1.007f,
        blueGain = 1.284f,
        brightness = -0.001f,
        contrast = 0.094f,
        saturation = 0.957f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
