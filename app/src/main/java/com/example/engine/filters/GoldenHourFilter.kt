package com.example.engine.filters

/**
 * "Golden" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object GoldenHourFilter {
    val grade = FilterGrade(
        redGain = 1.2f,
        greenGain = 1.04f,
        blueGain = 0.78f,
        brightness = 0.08f,
        contrast = 0.08f,
        saturation = 1.15f
    )
}
