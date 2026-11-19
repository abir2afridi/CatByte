package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Old Honeymoon".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object OldHoneymoonFilter {
    val grade = FilterGrade(
        redGain = 1.224f,
        greenGain = 1.012f,
        blueGain = 0.776f,
        brightness = 0.04f,
        contrast = 0.085f,
        saturation = 1.033f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
