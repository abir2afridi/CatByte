package com.example.data.export

import android.graphics.Matrix
import androidx.media3.effect.MatrixTransformation

/**
 * A transformation that can change over time - used for the zoom / slide
 * transitions.
 */
class TimeTransformEffect(
    private val matrixAt: (Long) -> Matrix
) : MatrixTransformation {

    override fun getMatrix(presentationTimeUs: Long): Matrix = matrixAt(presentationTimeUs)

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}
