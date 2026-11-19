package com.example.engine.filters.hit

import com.example.engine.filters.FilterGrade

/**
 * Hit-tab filter: "Umber Bleached".
 * Look family: warm golden. The shared Hit.jpg cover is tinted with this grade
 * on the card, so the thumbnail shows exactly what the filter does; preview
 * and export use these same numbers. All numbers live in THIS file only.
 */
object UmberBleachedFilter {
    val grade = FilterGrade(
        redGain = 1.184f,
        greenGain = 0.992f,
        blueGain = 0.706f,
        brightness = -0.006f,
        contrast = 0.142f,
        saturation = 1.061f,
        cardColorA = 0xFFC86B74,
        cardColorB = 0xFF2B2B33,
        tabs = listOf("Hit")
    )
}
