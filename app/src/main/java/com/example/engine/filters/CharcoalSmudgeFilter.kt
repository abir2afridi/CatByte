package com.example.engine.filters

/**
 * "Charcoal" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object CharcoalSmudgeFilter {
    val grade = FilterGrade(
        redGain = 1.0f,
        greenGain = 1.0f,
        blueGain = 1.0f,
        brightness = -0.1f,
        contrast = 0.42f,
        saturation = 0.0f
    )
}
