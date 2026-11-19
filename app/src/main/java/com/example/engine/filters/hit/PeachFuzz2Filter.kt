package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Peach Fuzz 2".
 * Look family: pink-violet tint. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PeachFuzz2Filter {
    val grade = FilterGrade(
        redGain = 1.14f,
        greenGain = 0.91f,
        blueGain = 1.135f,
        brightness = 0.003f,
        contrast = 0.112f,
        saturation = 1.246f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
