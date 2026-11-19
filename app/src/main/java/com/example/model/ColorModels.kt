package com.example.model

import kotlin.math.abs

enum class HslChannel(val displayName: String, val colorHex: Long) {
    RED("Red", 0xFFFF0000),
    ORANGE("Orange", 0xFFFFA500),
    YELLOW("Yellow", 0xFFFFFF00),
    GREEN("Green", 0xFF00FF00),
    CYAN("Cyan", 0xFF00FFFF),
    BLUE("Blue", 0xFF0000FF),
    PURPLE("Purple", 0xFF800080),
    MAGENTA("Magenta", 0xFFFF00FF)
}

data class HslAdjustment(
    val channel: HslChannel,
    val hue: Float = 0f,        // -180f to +180f
    val saturation: Float = 0f, // -100f to +100f
    val luminance: Float = 0f   // -100f to +100f
) {
    /** True when this channel does not change the picture at all. */
    val isNeutral: Boolean
        get() = abs(hue) < 0.01f && abs(saturation) < 0.01f && abs(luminance) < 0.01f

    /**
     * The values inside the supported range.
     *
     * NaN and infinity fall back to 0, so a malformed project cannot push the
     * colour grade into an undefined state.
     */
    fun canonical(): HslAdjustment = HslAdjustment(
        channel = channel,
        hue = hue.bounded(-180f, 180f),
        saturation = saturation.bounded(-100f, 100f),
        luminance = luminance.bounded(-100f, 100f)
    )

    private fun Float.bounded(min: Float, max: Float): Float =
        if (!isFinite()) 0f else coerceIn(min, max)
}

data class ChromaKeyData(
    val isEnabled: Boolean = false,
    val keyColorHex: Long = 0xFF00FF00, // Green
    val tolerance: Float = 0.25f,
    val softness: Float = 0.20f,
    val spill: Float = 0.30f
)