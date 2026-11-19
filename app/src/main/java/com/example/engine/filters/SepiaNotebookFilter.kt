package com.example.engine.filters

/**
 * "Sepia Paper" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object SepiaNotebookFilter {
    val grade = FilterGrade(
        redGain = 1.22f,
        greenGain = 1.02f,
        blueGain = 0.72f,
        brightness = 0.05f,
        contrast = 0.05f,
        cardColorA = 0xFF78350F,
        cardColorB = 0xFFFDE68A,
        tabs = listOf("Pencil", "Vintage"),
        saturation = 0.7f
    )
}
