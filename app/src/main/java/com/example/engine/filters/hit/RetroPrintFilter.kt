package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Retro Print".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object RetroPrintFilter {
    val grade = FilterGrade(
        redGain = 1.086f,
        greenGain = 0.953f,
        blueGain = 0.786f,
        brightness = -0.036f,
        contrast = -0.158f,
        saturation = 0.601f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
