package com.example.data

import com.example.data.export.ColorGradeEffect
import com.example.data.export.FilterMapper
import com.example.data.export.HslColorEffect
import com.example.engine.ColorAdjustMath
import com.example.model.MediaClip

/** Resolves filter + manual grade + HSL adjustments into one colour matrix. */
internal fun ProjectCompositionFactory.buildColorEffect(clip: MediaClip): ColorGradeEffect {
    val grade = FilterMapper.gradeFor(clip.filter)
    val contrast = (clip.contrast - 1f).coerceIn(-1f, 1f)
    val saturation = (clip.saturation * grade.saturation).coerceIn(0f, 2f)
    val hsl = HslColorEffect(clip.hslAdjustments)

    // The extra grade controls fold into the same matrix: exposure as a
    // global gain, temperature/tint as channel gains, vibrance as extra
    // saturation. Neutral values leave every factor at exactly 1.
    val exposureGain = ColorAdjustMath.exposureGain(clip.exposure)
    val (temperatureRed, temperatureBlue) = ColorAdjustMath.temperatureGains(clip.temperature)
    val tintGreen = ColorAdjustMath.tintGain(clip.tint)
    val vibrance = ColorAdjustMath.vibranceSaturation(clip.vibrance)

    return ColorGradeEffect(
        brightness = (clip.brightness + grade.brightness + hsl.brightnessOffset()).coerceIn(-1f, 1f),
        contrast = (contrast + grade.contrast).coerceIn(-1f, 1f),
        saturation = (saturation * hsl.saturationMultiplier() * vibrance).coerceIn(0f, 2f),
        redGain = grade.redGain * hsl.redMultiplier() * exposureGain * temperatureRed,
        greenGain = grade.greenGain * hsl.greenMultiplier() * exposureGain * tintGreen,
        blueGain = grade.blueGain * hsl.blueMultiplier() * exposureGain * temperatureBlue
    )
}
