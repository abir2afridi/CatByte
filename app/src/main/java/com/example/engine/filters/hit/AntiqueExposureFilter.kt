package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Antique Exposure".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object AntiqueExposureFilter {
    val grade = FilterGrade(
        redGain = 1.165f,
        greenGain = 0.992f,
        blueGain = 0.866f,
        brightness = 0.017f,
        contrast = -0.051f,
        saturation = 0.734f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
