package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Tropical Hues".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object TropicalHuesFilter {
    val grade = FilterGrade(
        redGain = 0.997f,
        greenGain = 0.968f,
        blueGain = 1.044f,
        brightness = 0.073f,
        contrast = 0.182f,
        saturation = 1.111f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
