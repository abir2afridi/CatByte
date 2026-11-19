package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Pure Joy".
 * Look family: soft pastel. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PureJoyFilter {
    val grade = FilterGrade(
        redGain = 1.072f,
        greenGain = 1.026f,
        blueGain = 1.113f,
        brightness = 0.169f,
        contrast = 0.004f,
        saturation = 0.712f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
