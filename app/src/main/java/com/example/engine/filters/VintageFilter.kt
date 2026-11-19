package com.example.engine.filters

/**
 * "Vintage" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object VintageFilter {
    val grade = FilterGrade(
        redGain = 1.14f,
        greenGain = 1.02f,
        blueGain = 0.86f,
        brightness = 0.04f,
        contrast = -0.06f,
        cardColorA = 0xFF8D6E63,
        cardColorB = 0xFFFFE082,
        tabs = listOf("Trending", "Vintage"),
        saturation = 0.8f
    )
}
