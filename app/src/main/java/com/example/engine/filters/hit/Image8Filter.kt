package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Image 8".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object Image8Filter {
    val grade = FilterGrade(
        redGain = 0.994f,
        greenGain = 0.967f,
        blueGain = 1.044f,
        brightness = -0.008f,
        contrast = 0.141f,
        saturation = 1.17f,
        cardColorA = 0xFF6BC8C0,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
