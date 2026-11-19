package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Old Friend".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object OldFriendFilter {
    val grade = FilterGrade(
        redGain = 1.064f,
        greenGain = 0.942f,
        blueGain = 0.781f,
        brightness = -0.039f,
        contrast = -0.16f,
        saturation = 0.68f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
