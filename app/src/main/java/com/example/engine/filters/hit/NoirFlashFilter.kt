package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Noir Flash".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object NoirFlashFilter {
    val grade = FilterGrade(
        redGain = 0.878f,
        greenGain = 0.899f,
        blueGain = 0.979f,
        brightness = -0.1f,
        contrast = 0.38f,
        saturation = 0.79f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
