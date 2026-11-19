package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Physique II".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PhysiqueIiFilter {
    val grade = FilterGrade(
        redGain = 1.041f,
        greenGain = 0.991f,
        blueGain = 0.995f,
        brightness = -0.04f,
        contrast = 0.045f,
        saturation = 1.123f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
