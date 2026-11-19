package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Deep Cyan".
 * Look family: cool cyan. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object DeepCyanFilter {
    val grade = FilterGrade(
        redGain = 0.812f,
        greenGain = 1.016f,
        blueGain = 1.288f,
        brightness = 0.082f,
        contrast = 0.136f,
        saturation = 0.978f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
