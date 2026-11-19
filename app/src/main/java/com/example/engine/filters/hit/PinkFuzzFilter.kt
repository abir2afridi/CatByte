package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Pink Fuzz".
 * Look family: pink-violet tint. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PinkFuzzFilter {
    val grade = FilterGrade(
        redGain = 1.17f,
        greenGain = 0.925f,
        blueGain = 1.203f,
        brightness = 0.129f,
        contrast = 0.174f,
        saturation = 1.278f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
