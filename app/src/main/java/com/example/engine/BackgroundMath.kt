package com.example.engine

import kotlin.math.abs

/**
 * Geometry of the "canvas background" feature (CapCut's Background/Canvas):
 * a video that does not fill the canvas leaves bars around it, and the
 * background (colour, image or blur) shows through those bars.
 *
 * The fitted rectangle is computed with the same "contain" rule Media3's
 * `Presentation.createForWidthAndHeight(..., LAYOUT_SCALE_TO_FIT)` uses, so
 * the shader's idea of where the video sits always matches the real pixels.
 */
object BackgroundMath {

    /** A rectangle in normalised 0..1 canvas coordinates. */
    data class NormalizedRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }

    /**
     * The contain-fit rectangle of a source of [sourceAspect] inside a canvas
     * of [canvasAspect]. Invalid aspects (unknown source size, zero or negative)
     * return a full-canvas rect, i.e. "no bars to fill".
     */
    fun fittedVideoRect(sourceAspect: Float, canvasAspect: Float): NormalizedRect {
        if (!isUsableAspect(sourceAspect) || !isUsableAspect(canvasAspect)) {
            return NormalizedRect(0f, 0f, 1f, 1f)
        }
        return if (sourceAspect > canvasAspect) {
            // Wider than the canvas: full width, letterboxed vertically.
            val height = canvasAspect / sourceAspect
            NormalizedRect(0f, (1f - height) / 2f, 1f, (1f + height) / 2f)
        } else {
            // Taller than the canvas: full height, pillarboxed horizontally.
            val width = sourceAspect / canvasAspect
            NormalizedRect((1f - width) / 2f, 0f, (1f + width) / 2f, 1f)
        }
    }

    /**
     * The x/y scale factors [androidx.media3.effect.GaussianBlurWithFrameOverlaid]
     * expects for a sharp centre of a video of [sourceAspect] on a canvas of
     * [canvasAspect]. Invalid aspects degrade to 1 (no sharp centre).
     */
    fun blurSharpScales(sourceAspect: Float, canvasAspect: Float): Pair<Float, Float> {
        if (!isUsableAspect(sourceAspect) || !isUsableAspect(canvasAspect)) {
            return 1f to 1f
        }
        return if (sourceAspect > canvasAspect) {
            1f to (canvasAspect / sourceAspect)
        } else {
            (sourceAspect / canvasAspect) to 1f
        }
    }

    /** "Cover" mapping of normalised canvas coordinates into image coordinates. */
    fun coverMappedUv(u: Float, v: Float, canvasAspect: Float, imageAspect: Float): Pair<Float, Float> {
        if (!isUsableAspect(canvasAspect) || !isUsableAspect(imageAspect)) {
            return u to v
        }
        return if (imageAspect > canvasAspect) {
            // Image is wider: crop the sides.
            ((u - 0.5f) * (canvasAspect / imageAspect) + 0.5f) to v
        } else {
            // Image is taller: crop the top and bottom.
            u to ((v - 0.5f) * (imageAspect / canvasAspect) + 0.5f)
        }
    }

    private fun isUsableAspect(aspect: Float): Boolean =
        aspect.isFinite() && aspect > 0f && abs(aspect - 0f) > 0.0001f
}
