package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Intimate Nostalgia".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object IntimateNostalgiaFilter {
    val grade = FilterGrade(
        redGain = 1.028f,
        greenGain = 1.065f,
        blueGain = 1.132f,
        brightness = 0.102f,
        contrast = -0.029f,
        saturation = 0.695f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
