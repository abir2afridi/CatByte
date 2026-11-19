package com.example.engine.filters

/**
 * "Manga Dots" filter - the ONLY definition of this look. The export renderer
 * and the preview both read [grade], so tuning these five numbers changes the
 * filter everywhere and touches nothing else.
 */
object HalftoneMangaFilter {
    val grade = FilterGrade(
        redGain = 1.0f,
        greenGain = 1.0f,
        blueGain = 1.0f,
        brightness = 0.08f,
        contrast = 0.3f,
        cardColorA = 0xFF18181B,
        cardColorB = 0xFFE4E4E7,
        tabs = listOf("Pencil", "B&W"),
        saturation = 0.25f
    )
}
