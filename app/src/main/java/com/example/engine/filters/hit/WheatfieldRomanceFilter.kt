package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Wheatfield Romance".
 * Look family: pink-violet tint. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object WheatfieldRomanceFilter {
    val grade = FilterGrade(
        redGain = 1.135f,
        greenGain = 0.908f,
        blueGain = 1.194f,
        brightness = 0.123f,
        contrast = 0.172f,
        saturation = 1.276f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
