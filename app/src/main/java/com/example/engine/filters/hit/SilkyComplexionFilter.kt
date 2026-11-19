package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Silky Complexion".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SilkyComplexionFilter {
    val grade = FilterGrade(
        redGain = 1.105f,
        greenGain = 1.103f,
        blueGain = 1.091f,
        brightness = 0.154f,
        contrast = -0.003f,
        saturation = 0.708f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
