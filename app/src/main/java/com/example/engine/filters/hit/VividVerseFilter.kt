package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Vivid Verse".
 * Look family: vivid neon. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object VividVerseFilter {
    val grade = FilterGrade(
        redGain = 1.172f,
        greenGain = 0.996f,
        blueGain = 1.258f,
        brightness = 0.112f,
        contrast = 0.346f,
        saturation = 1.723f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
