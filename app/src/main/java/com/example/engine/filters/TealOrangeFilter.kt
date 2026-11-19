package com.example.engine.filters

/**
 * "Blockbuster" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object TealOrangeFilter {
    val grade = FilterGrade(
        redGain = 0.92f,
        greenGain = 1.03f,
        blueGain = 1.12f,
        brightness = 0.04f,
        contrast = 0.18f,
        cardColorA = 0xFF00B4D8,
        cardColorB = 0xFFFF6B6B,
        tabs = listOf("Trending", "Cinematic"),
        saturation = 1.2f
    )
}
