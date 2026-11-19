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
        saturation = 0.25f
    )
}
