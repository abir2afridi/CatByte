package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Vintage Laze".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object VintageLazeFilter {
    val grade = FilterGrade(
        redGain = 1.077f,
        greenGain = 0.948f,
        blueGain = 0.784f,
        brightness = 0.043f,
        contrast = -0.119f,
        saturation = 0.701f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
