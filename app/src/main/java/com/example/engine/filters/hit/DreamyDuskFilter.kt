package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Dreamy Dusk".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object DreamyDuskFilter {
    val grade = FilterGrade(
        redGain = 1.126f,
        greenGain = 1.113f,
        blueGain = 1.157f,
        brightness = 0.198f,
        contrast = 0.019f,
        saturation = 0.72f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
