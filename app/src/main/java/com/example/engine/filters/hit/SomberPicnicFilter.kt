package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Somber Picnic".
 * Look family: balanced colour. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object SomberPicnicFilter {
    val grade = FilterGrade(
        redGain = 1.081f,
        greenGain = 1.01f,
        blueGain = 1.065f,
        brightness = 0.087f,
        contrast = 0.189f,
        saturation = 1.194f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFFF2E6D8,
        tabs = listOf("Hit")
    )
}
