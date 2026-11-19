package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Gritty Texture".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object GrittyTextureFilter {
    val grade = FilterGrade(
        redGain = 1.173f,
        greenGain = 1.056f,
        blueGain = 0.898f,
        brightness = 0.038f,
        contrast = -0.041f,
        saturation = 0.74f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
