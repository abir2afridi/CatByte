package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Digicam Soft".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object DigicamSoftFilter {
    val grade = FilterGrade(
        redGain = 1.038f,
        greenGain = 1.069f,
        blueGain = 1.074f,
        brightness = 0.063f,
        contrast = -0.129f,
        saturation = 0.726f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
