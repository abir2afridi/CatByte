package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Old Flame".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object OldFlameFilter {
    val grade = FilterGrade(
        redGain = 1.185f,
        greenGain = 1.053f,
        blueGain = 0.796f,
        brightness = 0.134f,
        contrast = 0.132f,
        saturation = 1.136f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
