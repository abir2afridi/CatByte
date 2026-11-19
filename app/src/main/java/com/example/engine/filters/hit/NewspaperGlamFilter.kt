package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Newspaper Glam".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object NewspaperGlamFilter {
    val grade = FilterGrade(
        redGain = 1.098f,
        greenGain = 0.959f,
        blueGain = 0.789f,
        brightness = -0.034f,
        contrast = -0.077f,
        saturation = 0.722f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
