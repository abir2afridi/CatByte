package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Cinematic Blues".
 * Look family: cool cyan. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object CinematicBluesFilter {
    val grade = FilterGrade(
        redGain = 0.778f,
        greenGain = 0.939f,
        blueGain = 1.189f,
        brightness = -0.064f,
        contrast = 0.063f,
        saturation = 1.022f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
