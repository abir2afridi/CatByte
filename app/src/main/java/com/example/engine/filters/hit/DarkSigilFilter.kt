package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Dark Sigil".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object DarkSigilFilter {
    val grade = FilterGrade(
        redGain = 0.812f,
        greenGain = 0.806f,
        blueGain = 0.993f,
        brightness = -0.172f,
        contrast = 0.344f,
        saturation = 0.692f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
