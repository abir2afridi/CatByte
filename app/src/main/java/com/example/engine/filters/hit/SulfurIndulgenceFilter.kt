package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Sulfur Indulgence".
 * Look family: green cast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SulfurIndulgenceFilter {
    val grade = FilterGrade(
        redGain = 0.817f,
        greenGain = 1.138f,
        blueGain = 0.809f,
        brightness = -0.054f,
        contrast = 0.082f,
        saturation = 1.052f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
