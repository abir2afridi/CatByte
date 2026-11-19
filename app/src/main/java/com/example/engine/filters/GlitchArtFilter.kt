package com.example.engine.filters

/**
 * "Glitch Art" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object GlitchArtFilter {
    val grade = FilterGrade(
        redGain = 1.12f,
        greenGain = 0.94f,
        blueGain = 1.18f,
        brightness = 0.02f,
        contrast = 0.32f,
        saturation = 1.45f
    )
}
