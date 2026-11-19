package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Sunkissed Peach".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SunkissedPeachFilter {
    val grade = FilterGrade(
        redGain = 1.196f,
        greenGain = 1.058f,
        blueGain = 0.799f,
        brightness = 0.056f,
        contrast = 0.173f,
        saturation = 1.076f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
