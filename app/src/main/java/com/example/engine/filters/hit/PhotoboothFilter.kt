package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Photobooth".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PhotoboothFilter {
    val grade = FilterGrade(
        redGain = 1.179f,
        greenGain = 1.06f,
        blueGain = 0.9f,
        brightness = 0.12f,
        contrast = -0.08f,
        saturation = 0.72f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
