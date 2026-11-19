package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Retro Darling".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object RetroDarlingFilter {
    val grade = FilterGrade(
        redGain = 1.107f,
        greenGain = 0.963f,
        blueGain = 0.791f,
        brightness = -0.032f,
        contrast = -0.076f,
        saturation = 0.642f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
