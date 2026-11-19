package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Sepia Grain".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SepiaGrainFilter {
    val grade = FilterGrade(
        redGain = 1.128f,
        greenGain = 1.034f,
        blueGain = 0.827f,
        brightness = -0.009f,
        contrast = -0.064f,
        saturation = 0.728f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
