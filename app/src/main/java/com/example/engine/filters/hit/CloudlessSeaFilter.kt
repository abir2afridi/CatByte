package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Cloudless Sea".
 * Look family: cool cyan. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object CloudlessSeaFilter {
    val grade = FilterGrade(
        redGain = 0.851f,
        greenGain = 1.036f,
        blueGain = 1.238f,
        brightness = -0.032f,
        contrast = 0.079f,
        saturation = 0.949f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
