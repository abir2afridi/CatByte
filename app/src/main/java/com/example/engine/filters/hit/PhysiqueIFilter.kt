package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Physique I".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object PhysiqueIFilter {
    val grade = FilterGrade(
        redGain = 1.026f,
        greenGain = 1.043f,
        blueGain = 1.021f,
        brightness = -0.022f,
        contrast = 0.134f,
        saturation = 1.087f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
