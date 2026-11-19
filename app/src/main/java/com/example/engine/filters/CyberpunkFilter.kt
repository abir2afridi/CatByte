package com.example.engine.filters

/**
 * "Cyberpunk" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object CyberpunkFilter {
    val grade = FilterGrade(
        redGain = 0.88f,
        greenGain = 0.95f,
        blueGain = 1.3f,
        brightness = -0.04f,
        contrast = 0.26f,
        cardColorA = 0xFF00FFFF,
        cardColorB = 0xFFFF00FF,
        tabs = listOf("Trending", "Neon"),
        saturation = 1.35f
    )
}
