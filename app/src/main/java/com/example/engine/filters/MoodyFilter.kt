package com.example.engine.filters

/**
 * "Moody" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object MoodyFilter {
    val grade = FilterGrade(
        redGain = 0.92f,
        greenGain = 0.97f,
        blueGain = 1.06f,
        brightness = -0.08f,
        contrast = 0.12f,
        cardColorA = 0xFF1E293B,
        cardColorB = 0xFF64748B,
        tabs = listOf("Cinematic", "Cool"),
        saturation = 0.55f
    )
}
