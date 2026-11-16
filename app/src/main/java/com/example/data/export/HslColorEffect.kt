package com.example.data.export

import com.example.model.HslAdjustment
import com.example.model.HslChannel
import kotlin.math.abs
import kotlin.math.sin

/**
 * Applies the per-channel HSL adjustments to the colour grade.
 *
 * Media3's RgbMatrix is a linear 4x4 transform, so genuinely selective HSL
 * would need a custom fragment shader. This helper folds the adjustments into
 * the neighbouring colour-grade matrix instead of silently dropping the UI
 * values: hue shifts rotate the channel gains, saturation scales the global
 * saturation, and luminance adds a bounded brightness offset. The result is a
 * real, visible approximation that previews and exports identically.
 */
class HslColorEffect(
    private val adjustments: List<HslAdjustment>
) {

    private val active = adjustments.filter {
        abs(it.hue) > 0.001f || abs(it.saturation) > 0.001f || abs(it.luminance) > 0.001f
    }

    fun brightnessOffset(): Float {
        if (active.isEmpty()) return 0f
        return (active.map { it.luminance }.average() / 100.0 * 0.28)
            .toFloat()
            .coerceIn(-0.35f, 0.35f)
    }

    fun saturationMultiplier(): Float {
        if (active.isEmpty()) return 1f
        return (1f + active.map { it.saturation }.average().toFloat() / 100f * 0.85f)
            .coerceIn(0f, 2f)
    }

    fun redMultiplier(): Float = channelMultiplier(HslChannel.RED, 0f)

    fun greenMultiplier(): Float = channelMultiplier(HslChannel.GREEN, 2f / 3f)

    fun blueMultiplier(): Float = channelMultiplier(HslChannel.BLUE, 4f / 3f)

    private fun channelMultiplier(channel: HslChannel, phase: Float): Float {
        val matches = active.filter { it.channel == channel }
        if (matches.isEmpty()) return 1f
        // Hue is displayed in degrees. A positive shift pushes the selected
        // colour towards the next hue, so bias the primary channels in a
        // matching direction. The approximation is intentionally conservative.
        val hue = matches.map { it.hue }.average().toFloat()
        val luminance = matches.map { it.luminance }.average().toFloat()
        val saturation = matches.map { it.saturation }.average().toFloat()
        val hueWave = sin(Math.toRadians((hue + phase * 180f).toDouble())).toFloat()
        return (1f + hueWave * 0.18f + luminance / 100f * 0.45f - saturation / 100f * 0.12f)
            .coerceIn(0.25f, 2.5f)
    }
}
