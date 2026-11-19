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
        cardColorA = 0xFF2C2B28,
        cardColorB = 0xFFD6CEBD,
        tabs = listOf("Pencil", "B&W"),
        saturation = 0.15f
    )
}
