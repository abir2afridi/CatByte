package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Fresh Lime".
 * Look family: green cast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object FreshLimeFilter {
    val grade = FilterGrade(
        redGain = 0.784f,
        greenGain = 1.182f,
        blueGain = 0.831f,
        brightness = -0.039f,
        contrast = 0.09f,
        saturation = 1.055f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
