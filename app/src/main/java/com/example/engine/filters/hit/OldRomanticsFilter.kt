package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Old Romantics".
 * Look family: pink-violet tint. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object OldRomanticsFilter {
    val grade = FilterGrade(
        redGain = 1.163f,
        greenGain = 0.861f,
        blueGain = 1.11f,
        brightness = 0.067f,
        contrast = 0.063f,
        saturation = 1.222f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
