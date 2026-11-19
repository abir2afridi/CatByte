package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Fairy's Meadow".
 * Look family: green cast. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object FairysMeadowFilter {
    val grade = FilterGrade(
        redGain = 0.831f,
        greenGain = 1.145f,
        blueGain = 0.813f,
        brightness = -0.052f,
        contrast = 0.084f,
        saturation = 0.972f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
