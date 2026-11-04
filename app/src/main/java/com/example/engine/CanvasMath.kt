package com.example.engine

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * On-canvas gesture maths for a layer: drag, pinch (scale about the finger
 * centroid) and rotate. Values are normalised so the same numbers work at any
 * preview size and in the export compositor.
 */
data class LayerTransform(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
) {
    init {
        require(scale > 0f) { "scale must be > 0 (was $scale)" }
    }
}

object CanvasMath {

    const val MIN_SCALE = 0.1f
    const val MAX_SCALE = 8f

    /** Short side of the output, in pixels. */
    fun shortSidePx(resolution: String): Int {
        val cleaned = resolution.uppercase().replace("P", "").replace(" ", "").trim()
        return when (cleaned) {
            "480", "480I" -> 480
            "720" -> 720
            "1080" -> 1080
            "1440", "2K" -> 1440
            "2160", "4K" -> 2160
            "4320", "8K" -> 4320
            else -> 1080
        }
    }

    /** Even width/height for the given aspect ratio; codecs require even sides. */
    fun canvasSize(aspect: com.example.model.AspectRatioType, resolution: String): Pair<Int, Int> {
        val short = shortSidePx(resolution)
        val ratioWidth = aspect.ratioWidth
        val ratioHeight = aspect.ratioHeight
        val long = (short.toDouble() * maxOf(ratioWidth, ratioHeight) / minOf(ratioWidth, ratioHeight))
            .roundToInt()
        val width: Int
        val height: Int
        when {
            ratioWidth > ratioHeight -> {
                width = long
                height = short
            }
            ratioWidth < ratioHeight -> {
                width = short
                height = long
            }
            else -> {
                width = short
                height = short
            }
        }
        return even(width) to even(height)
    }

    private fun even(value: Int): Int = if (value % 2 == 0) value else value + 1

    fun normaliseRotation(deg: Float): Float {
        var value = deg % 360f
        if (value > 180f) value -= 360f
        if (value < -180f) value += 360f
        return value
    }

    fun drag(start: LayerTransform, dxNorm: Float, dyNorm: Float): LayerTransform =
        start.copy(centerX = start.centerX + dxNorm, centerY = start.centerY + dyNorm)

    /**
     * Pinch/rotate about a focal point. The content under the focal point stays
     * put: the layer centre is rotated and scaled around it, which is what makes
     * two-finger zoom feel anchored to the fingers instead of the frame centre.
     */
    fun pinch(
        start: LayerTransform,
        focalX: Float,
        focalY: Float,
        scaleFactor: Float,
        rotationDeltaDeg: Float,
        minScale: Float = MIN_SCALE,
        maxScale: Float = MAX_SCALE,
    ): LayerTransform {
        val newScale = (start.scale * scaleFactor).coerceIn(minScale, maxScale)
        val ratio = newScale / start.scale
        val dx = start.centerX - focalX
        val dy = start.centerY - focalY
        val rad = Math.toRadians(rotationDeltaDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val rotX = dx * cosA - dy * sinA
        val rotY = dx * sinA + dy * cosA
        return LayerTransform(
            centerX = focalX + rotX * ratio,
            centerY = focalY + rotY * ratio,
            scale = newScale,
            rotationDeg = normaliseRotation(start.rotationDeg + rotationDeltaDeg),
        )
    }

    /**
     * Keeps at least [minVisible] of the layer on canvas, so a layer can be
     * pushed mostly off-screen but never lost entirely.
     */
    fun clampInside(transform: LayerTransform, minVisible: Float = 0.1f): LayerTransform {
        val half = 0.5f * transform.scale
        val lo = minVisible - half
        val hi = 1f - minVisible + half
        return transform.copy(
            centerX = transform.centerX.coerceIn(lo, hi),
            centerY = transform.centerY.coerceIn(lo, hi),
        )
    }

    /** Snaps the layer centre to the canvas centre line(s) inside a threshold. */
    fun snapToCenter(
        transform: LayerTransform,
        thresholdNorm: Float = 0.02f,
        snapX: Boolean = true,
        snapY: Boolean = true,
    ): LayerTransform {
        val x = if (snapX && abs(transform.centerX - 0.5f) <= thresholdNorm) 0.5f else transform.centerX
        val y = if (snapY && abs(transform.centerY - 0.5f) <= thresholdNorm) 0.5f else transform.centerY
        return transform.copy(centerX = x, centerY = y)
    }

    /** Scale that makes content fit inside the canvas without cropping (contain). */
    fun aspectFitScale(contentWidth: Int, contentHeight: Int, canvasWidth: Int, canvasHeight: Int): Float {
        if (contentWidth <= 0 || contentHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) return 1f
        val sx = canvasWidth.toFloat() / contentWidth
        val sy = canvasHeight.toFloat() / contentHeight
        return minOf(sx, sy)
    }

    /** Scale that fills the canvas, cropping the overflowing axis (cover). */
    fun aspectFillScale(contentWidth: Int, contentHeight: Int, canvasWidth: Int, canvasHeight: Int): Float {
        if (contentWidth <= 0 || contentHeight <= 0 || canvasWidth <= 0 || canvasHeight <= 0) return 1f
        val sx = canvasWidth.toFloat() / contentWidth
        val sy = canvasHeight.toFloat() / contentHeight
        return maxOf(sx, sy)
    }

    fun hasContentNear(transform: LayerTransform, u: Float, v: Float, tolerance: Float = 0.05f): Boolean =
        hypot(transform.centerX - u, transform.centerY - v) <= 0.5f * transform.scale + tolerance
}

/**
 * Crop maths. Everything is normalised against the source frame (0..1), so the
 * same crop survives a preview resize or a 4K export.
 */
object CropMath {

    data class Crop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val aspect: Float get() = if (height <= 0f) 0f else width / height
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f
    }

    val FULL = Crop(0f, 0f, 1f, 1f)

    /** Largest rectangle of [aspect] inside the unit frame, centred on (cx, cy). */
    fun byAspect(aspect: Float, centerX: Float = 0.5f, centerY: Float = 0.5f, coverage: Float = 1f): Crop {
        require(aspect > 0f) { "aspect must be > 0 (was $aspect)" }
        val c = coverage.coerceIn(0.05f, 1f)
        val width: Float
        val height: Float
        if (aspect >= 1f) {
            width = c
            height = c / aspect
        } else {
            height = c
            width = c * aspect
        }
        return clamp(Crop(centerX - width / 2f, centerY - height / 2f, centerX + width / 2f, centerY + height / 2f))
    }

    fun clamp(crop: Crop, minSize: Float = 0.02f): Crop {
        val w = crop.width.coerceIn(minSize, 1f)
        val h = crop.height.coerceIn(minSize, 1f)
        var left = crop.left.coerceIn(0f, 1f - w)
        var top = crop.top.coerceIn(0f, 1f - h)
        if (left + w > 1f) left = 1f - w
        if (top + h > 1f) top = 1f - h
        return Crop(left, top, left + w, top + h)
    }

    /**
     * A 90/270 degree source rotation swaps the crop's axes; the rectangle is
     * re-expressed on the rotated frame about its own centre.
     */
    fun rotated(crop: Crop, rotationDeg: Float): Crop {
        val deg = ((rotationDeg % 360f) + 360f) % 360f
        if (abs(deg - 90f) > 0.001f && abs(deg - 270f) > 0.001f) return crop
        val cx = crop.centerX
        val cy = crop.centerY
        val w = crop.height
        val h = crop.width
        return clamp(Crop(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f))
    }

    /** Pixel rect as [left, top, width, height], even-aligned for the encoder. */
    fun toPixels(crop: Crop, frameWidth: Int, frameHeight: Int, even: Boolean = true): IntArray {
        require(frameWidth > 0 && frameHeight > 0) { "frame must be positive" }
        // Compute right/bottom from the absolute edges, not from width/height
        // products: 0.9f - 0.1f is 0.79999995 in float, and truncating either
        // leg produced 1534px instead of 1536px.
        var left = (crop.left * frameWidth).roundToInt()
        var top = (crop.top * frameHeight).roundToInt()
        var right = (crop.right * frameWidth).roundToInt()
        var bottom = (crop.bottom * frameHeight).roundToInt()
        var width = (right - left).coerceIn(2, frameWidth)
        var height = (bottom - top).coerceIn(2, frameHeight)
        if (even) {
            left -= left % 2
            top -= top % 2
            right = left + width
            bottom = top + height
            // Snap the far edge to even, growing the size by one when the two
            // rounded legs land on opposite parities. Encoders require an even
            // *size*, not the floor of the rounded size.
            if ((right % 2) != 0) width += 1
            if ((bottom % 2) != 0) height += 1
            right = left + width
            bottom = top + height
        }
        width = width.coerceIn(2, frameWidth)
        height = height.coerceIn(2, frameHeight)
        left = left.coerceIn(0, frameWidth - width)
        top = top.coerceIn(0, frameHeight - height)
        return intArrayOf(left, top, width, height)
    }
}
