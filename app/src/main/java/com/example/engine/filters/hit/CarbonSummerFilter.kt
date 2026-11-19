package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Carbon Summer".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object CarbonSummerFilter {
    val grade = FilterGrade(
        redGain = 1.08f,
        greenGain = 1.07f,
        blueGain = 1.095f,
        brightness = 0.107f,
        contrast = 0.199f,
        saturation = 1.199f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
