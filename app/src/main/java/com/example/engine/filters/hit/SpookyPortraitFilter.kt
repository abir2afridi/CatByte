package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Spooky Portrait".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SpookyPortraitFilter {
    val grade = FilterGrade(
        redGain = 0.821f,
        greenGain = 0.87f,
        blueGain = 1.025f,
        brightness = -0.069f,
        contrast = 0.315f,
        saturation = 0.758f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
