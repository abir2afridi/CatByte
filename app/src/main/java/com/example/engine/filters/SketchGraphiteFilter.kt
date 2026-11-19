package com.example.engine.filters

/**
 * "2B Graphite" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object SketchGraphiteFilter {
    val grade = FilterGrade(
        redGain = 0.94f,
        greenGain = 0.96f,
        blueGain = 1.0f,
        brightness = 0.06f,
        contrast = 0.22f,
        saturation = 0.15f
    )
}
