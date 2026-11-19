package com.example.data.export

import androidx.media3.effect.RgbMatrix
import com.example.engine.ClipAnimationMath

/**
 * The "fade in" entry animation: the clip really starts black and comes up
 * to its normal brightness over [durationMs].
 *
 * Media3 samples an [RgbMatrix] once per frame, so the progress curve is
 * evaluated against the frame's own presentation time. The shader multiplies
 * the colour channels with the matrix and restores the alpha itself, so
 * only the three diagonal colour entries matter here - the clip stays
 * opaque and fades out of black, which is what an entry fade looks like on
 * the canvas.
 *
 * The same effect is used by the CompositionPlayer preview and by the
 * Transformer export, because both build their effect list from the shared
 * composition factory.
 */
class ClipFadeInEffect(private val durationMs: Long) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useLinearToneMapping: Boolean): FloatArray {
        val progress = ClipAnimationMath.fadeProgress(presentationTimeUs / 1000L, durationMs)
        // Media3's colour matrix is column major: the diagonal entries are
        // the red, green and blue gains.
        return floatArrayOf(
            progress, 0f, 0f, 0f,
            0f, progress, 0f, 0f,
            0f, 0f, progress, 0f,
            0f, 0f, 0f, 1f
        )
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = durationMs <= 0L
}
