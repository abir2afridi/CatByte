package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Velvet Matte".
 * Look family: faded vintage. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object VelvetMatteFilter {
    val grade = FilterGrade(
        redGain = 1.11f,
        greenGain = 0.965f,
        blueGain = 0.792f,
        brightness = 0.048f,
        contrast = -0.036f,
        saturation = 0.662f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
