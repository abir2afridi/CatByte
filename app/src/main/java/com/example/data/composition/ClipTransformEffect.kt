package com.example.data.composition

import android.graphics.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import com.example.data.KeyframeInterpolator
import com.example.engine.ClipAnimationMath
import com.example.model.MediaClip
import kotlin.math.abs

/** Main-track transform with per-frame keyframe support. */
@UnstableApi
internal class ClipTransformEffect(private val clip: MediaClip) : MatrixTransformation {

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val timeMs = presentationTimeUs / 1000L
        // The clip owns the length of its entry animation, so the slider in
        // the animation panel really changes how long the clip takes to
        // arrive.
        val animation = ClipAnimationMath.transformAt(
            animation = clip.animationIn,
            timeMs = timeMs,
            durationMs = clip.animationDurationMs
        )
        val scale = KeyframeInterpolator.getScale(clip, timeMs).coerceAtLeast(0.01f) * animation.scale
        val rotation = KeyframeInterpolator.getRotation(clip, timeMs) + animation.rotationDeg
        val posX = KeyframeInterpolator.getPosX(clip, timeMs).coerceIn(0f, 1f)
        val posY = KeyframeInterpolator.getPosY(clip, timeMs).coerceIn(0f, 1f)

        val matrix = Matrix()
        val scaleX = if (clip.isMirrored) -scale else scale
        // MatrixTransformation operates in NDC; the origin is the frame centre.
        matrix.setScale(scaleX, scale, 0f, 0f)
        if (abs(rotation) > 0.001f) {
            matrix.postRotate(rotation, 0f, 0f)
        }
        val translateX = (posX - 0.5f) * 2f + animation.translateX
        val translateY = (0.5f - posY) * 2f + animation.translateY
        if (abs(translateX) > 0.0001f || abs(translateY) > 0.0001f) {
            matrix.postTranslate(translateX, translateY)
        }
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        clip.keyframes.isEmpty() &&
            clip.animationIn == com.example.model.ClipAnimation.NONE &&
            abs(clip.scale - 1f) < 0.0001f &&
            abs(clip.rotation) < 0.0001f &&
            !clip.isMirrored &&
            abs(clip.posX - 0.5f) < 0.0001f &&
            abs(clip.posY - 0.5f) < 0.0001f
}