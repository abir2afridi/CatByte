package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Pale Mist".
 * Look family: cool cyan. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PaleMistFilter {
    val grade = FilterGrade(
        redGain = 0.781f,
        greenGain = 0.941f,
        blueGain = 1.19f,
        brightness = -0.063f,
        contrast = 0.063f,
        saturation = 0.941f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
