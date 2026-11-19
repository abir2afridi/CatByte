package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Archaic Terror".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object ArchaicTerrorFilter {
    val grade = FilterGrade(
        redGain = 0.775f,
        greenGain = 0.848f,
        blueGain = 0.954f,
        brightness = -0.117f,
        contrast = 0.291f,
        saturation = 0.746f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
