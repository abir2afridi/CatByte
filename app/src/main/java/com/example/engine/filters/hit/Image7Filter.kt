package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Image 7".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object Image7Filter {
    val grade = FilterGrade(
        redGain = 0.987f,
        greenGain = 1.024f,
        blueGain = 1.072f,
        brightness = 0.011f,
        contrast = 0.07f,
        saturation = 1.135f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
