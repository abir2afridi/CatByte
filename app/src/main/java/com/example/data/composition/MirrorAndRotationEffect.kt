package com.example.data.composition

import android.graphics.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import com.example.model.MediaClip
import kotlin.math.abs

/** Mirror/rotation for PIP textures; positioning is handled by the compositor. */
@UnstableApi
internal class MirrorAndRotationEffect(private val clip: MediaClip) : MatrixTransformation {

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val matrix = Matrix()
        val scaleX = if (clip.isMirrored) -1f else 1f
        matrix.setScale(scaleX, 1f, 0f, 0f)
        if (abs(clip.rotation) > 0.001f) {
            matrix.postRotate(clip.rotation, 0f, 0f)
        }
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        !clip.isMirrored && abs(clip.rotation) < 0.001f
}