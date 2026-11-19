package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Wine Wash".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object WineWashFilter {
    val grade = FilterGrade(
        redGain = 1.197f,
        greenGain = 0.998f,
        blueGain = 0.769f,
        brightness = 0.116f,
        contrast = 0.203f,
        saturation = 1.172f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
