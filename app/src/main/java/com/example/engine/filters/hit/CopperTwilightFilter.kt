package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Copper Twilight".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object CopperTwilightFilter {
    val grade = FilterGrade(
        redGain = 1.222f,
        greenGain = 1.071f,
        blueGain = 0.745f,
        brightness = 0.1f,
        contrast = 0.115f,
        saturation = 1.128f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
