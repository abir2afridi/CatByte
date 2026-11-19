package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Nature's Kiss".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object NaturesKissFilter {
    val grade = FilterGrade(
        redGain = 1.043f,
        greenGain = 0.992f,
        blueGain = 0.996f,
        brightness = 0.04f,
        contrast = 0.166f,
        saturation = 1.183f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
