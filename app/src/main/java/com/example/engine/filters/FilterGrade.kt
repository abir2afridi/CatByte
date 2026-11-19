package com.example.engine.filters

/**
 * The colour grade numbers ONE filter applies. Every filter owns its own file
 * (engine/filters/<Name>Filter.kt) holding exactly one of these - so tuning
 * "Blueprint" can never change "Vintage".
 *
 * The same numbers drive BOTH renderers: the export path (ColorGradeEffect)
 * and the Compose preview ([toPreviewColorFilter]) share this definition, so
 * there is no second, drift-prone copy of a filter's look anywhere.
 */
data class FilterGrade(
    val redGain: Float,
    val greenGain: Float,
    val blueGain: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    /** Screen-tab swatch colours (argb) - owned by this filter's own file. */
    val cardColorA: Long = 0xFF2B2B33,
    val cardColorB: Long = 0xFFA5A8B2,
    /** drawer tabs this filter appears in. */
    val tabs: List<String> = listOf("All")
) {
    /** True when applying this grade changes nothing. */
    val isIdentity: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 1f &&
            redGain == 1f && greenGain == 1f && blueGain == 1f

    /**
     * The same matrix [com.example.data.export.ColorGradeEffect] builds for
     * the export, converted into the Compose 4x5 row-major layout (offsets
     * scaled to the 0..255 colour range).
     */
    fun toPreviewMatrix(): FloatArray {
        val b = brightness.coerceIn(-1f, 1f)
        val c = contrast.coerceIn(-1f, 1f)
        val s = saturation.coerceIn(0f, 2f)
        val contrastScale = (1f + c) / 1.0001f
        val offset = (-0.5f * contrastScale + 0.5f + b) * 255f

        val sr = 0.213f + 0.787f * s
        val sg = 0.715f - 0.715f * s
        val sb = 0.072f - 0.072f * s
        val gr = 0.213f - 0.213f * s
        val gg = 0.715f + 0.285f * s
        val gb = 0.072f - 0.072f * s
        val br = 0.213f - 0.213f * s
        val bg = 0.715f - 0.715f * s
        val bb = 0.072f + 0.928f * s

        return floatArrayOf(
            sr * contrastScale * redGain, sg * contrastScale * redGain,
            sb * contrastScale * redGain, 0f, offset * redGain,
            gr * contrastScale * greenGain, gg * contrastScale * greenGain,
            gb * contrastScale * greenGain, 0f, offset * greenGain,
            br * contrastScale * blueGain, bg * contrastScale * blueGain,
            bb * contrastScale * blueGain, 0f, offset * blueGain,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
