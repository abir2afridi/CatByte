package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Haunted Canvas".
 * Look family: dark high-contrast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object HauntedCanvasFilter {
    val grade = FilterGrade(
        redGain = 0.818f,
        greenGain = 0.869f,
        blueGain = 0.965f,
        brightness = -0.19f,
        contrast = 0.335f,
        saturation = 0.687f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
