package com.example.data.export

import androidx.media3.common.util.GlUtil
import androidx.media3.effect.RgbMatrix

/**
 * A colour grading effect that can be evaluated for every frame. Supports
 * brightness, contrast, saturation and per-channel gains (used by the filters).
 *
 * The matrix layout follows Media3's 4x4 column major convention:
 * ```
 * m[0]  m[1]  m[2]  m[3]    -> red
 * m[4]  m[5]  m[6]  m[7]    -> green
 * m[8]  m[9]  m[10] m[11]   -> blue
 * m[12] m[13] m[14] m[15]   -> alpha
 * ```
 */
class ColorGradeEffect(
    private val brightness: Float = 0f,
    private val contrast: Float = 0f,
    private val saturation: Float = 1f,
    private val redGain: Float = 1f,
    private val greenGain: Float = 1f,
    private val blueGain: Float = 1f
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useLinearToneMapping: Boolean): FloatArray {
        val matrix = GlUtil.create4x4IdentityMatrix()

        val b = brightness.coerceIn(-1f, 1f)
        val c = contrast.coerceIn(-1f, 1f)
        val s = saturation.coerceIn(0f, 2f)

        // Contrast around the 0.5 mid point.
        val contrastScale = (1f + c) / 1.0001f
        val offset = -0.5f * contrastScale + 0.5f + b

        // Rec.709 style saturation matrix (identity when s == 1).
        val sr = 0.213f + 0.787f * s
        val sg = 0.715f - 0.715f * s
        val sb = 0.072f - 0.072f * s
        val gr = 0.213f - 0.213f * s
        val gg = 0.715f + 0.285f * s
        val gb = 0.072f - 0.072f * s
        val br = 0.213f - 0.213f * s
        val bg = 0.715f - 0.715f * s
        val bb = 0.072f + 0.928f * s

        matrix[0] = sr * contrastScale * redGain
        matrix[1] = sg * contrastScale * redGain
        matrix[2] = sb * contrastScale * redGain
        matrix[3] = offset * redGain

        matrix[4] = gr * contrastScale * greenGain
        matrix[5] = gg * contrastScale * greenGain
        matrix[6] = gb * contrastScale * greenGain
        matrix[7] = offset * greenGain

        matrix[8] = br * contrastScale * blueGain
        matrix[9] = bg * contrastScale * blueGain
        matrix[10] = bb * contrastScale * blueGain
        matrix[11] = offset * blueGain

        matrix[15] = 1f
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return brightness == 0f && contrast == 0f && saturation == 1f &&
            redGain == 1f && greenGain == 1f && blueGain == 1f
    }
}
