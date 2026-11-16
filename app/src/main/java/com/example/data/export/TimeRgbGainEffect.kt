package com.example.data.export

import androidx.media3.common.util.GlUtil
import androidx.media3.effect.RgbMatrix

/**
 * A transition effect that changes the RGB gain over time. Used to burn the
 * fade / flash transitions into the exported video.
 */
class TimeRgbGainEffect(
    private val startUs: Long,
    private val endUs: Long,
    private val gainAt: (Long) -> FloatArray
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useLinearToneMapping: Boolean): FloatArray {
        val matrix = GlUtil.create4x4IdentityMatrix()
        val gain = gainAt(presentationTimeUs)
        matrix[0] = gain[0]
        matrix[5] = gain[1]
        matrix[10] = gain[2]
        matrix[15] = 1f
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}
