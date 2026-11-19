package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Wicked Forest".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object WickedForestFilter {
    val grade = FilterGrade(
        redGain = 0.879f,
        greenGain = 0.839f,
        blueGain = 0.95f,
        brightness = -0.12f,
        contrast = 0.29f,
        saturation = 0.745f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
