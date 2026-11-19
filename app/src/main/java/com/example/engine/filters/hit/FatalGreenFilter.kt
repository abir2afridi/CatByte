package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Fatal Green".
 * Look family: green cast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object FatalGreenFilter {
    val grade = FilterGrade(
        redGain = 0.789f,
        greenGain = 1.184f,
        blueGain = 0.832f,
        brightness = -0.039f,
        contrast = 0.171f,
        saturation = 1.015f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
