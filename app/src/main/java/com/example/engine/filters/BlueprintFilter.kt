package com.example.engine.filters

/**
 * "Blueprint" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object BlueprintFilter {
    val grade = FilterGrade(
        redGain = 0.42f,
        greenGain = 0.78f,
        blueGain = 1.25f,
        brightness = -0.04f,
        contrast = 0.18f,
        cardColorA = 0xFF0F172A,
        cardColorB = 0xFF38BDF8,
        tabs = listOf("Pencil", "Cool"),
        saturation = 0.85f
    )
}
