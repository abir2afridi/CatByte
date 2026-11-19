package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Image 6".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object Image6Filter {
    val grade = FilterGrade(
        redGain = 0.999f,
        greenGain = 0.969f,
        blueGain = 0.985f,
        brightness = 0.033f,
        contrast = 0.162f,
        saturation = 1.101f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
