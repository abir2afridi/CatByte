package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Image 5".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object Image5Filter {
    val grade = FilterGrade(
        redGain = 1.069f,
        greenGain = 1.064f,
        blueGain = 1.032f,
        brightness = 0.065f,
        contrast = 0.177f,
        saturation = 1.189f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
