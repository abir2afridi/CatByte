package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Light Pastel".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object LightPastelFilter {
    val grade = FilterGrade(
        redGain = 1.103f,
        greenGain = 1.102f,
        blueGain = 1.091f,
        brightness = 0.074f,
        contrast = -0.123f,
        saturation = 0.728f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
