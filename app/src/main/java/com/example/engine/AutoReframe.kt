package com.example.engine

import com.example.model.EasingCurve
import kotlin.math.max
import kotlin.math.min

/** Rectangle in normalized source coordinates (0..1, origin top-left). */
data class NormRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun clampedToUnit(): NormRect {
        val l = left.coerceIn(0f, 1f)
        val t = top.coerceIn(0f, 1f)
        val r = right.coerceIn(l, 1f)
        val b = bottom.coerceIn(t, 1f)
        return NormRect(l, t, r, b)
    }

    companion object {
        val FULL = NormRect(0f, 0f, 1f, 1f)
        val CENTER = NormRect(0.25f, 0.25f, 0.75f, 0.75f)

        /** Union of boxes, used to keep every face inside the crop. */
        fun union(boxes: List<NormRect>): NormRect {
            if (boxes.isEmpty()) return FULL
            return NormRect(
                boxes.minOf { it.left },
                boxes.minOf { it.top },
                boxes.maxOf { it.right },
                boxes.maxOf { it.bottom }
            ).clampedToUnit()
        }
    }
}

/** Detected subject box with a confidence score. */
data class SubjectBox(val rect: NormRect, val confidence: Float = 1f)

/** Pixel crop rectangle produced for the encoder. */
data class CropRect(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * Auto-reframe: keep the subject inside the new aspect ratio.
 *
 * Face boxes come from the on-device detector; this object decides the crop
 * window, smooths it over time (so the camera does not jitter) and animates the
 * Ken Burns move for stills.
 */
object AutoReframe {

    /**
     * Crop window with [targetAspect] that contains the subject. The window is
     * always inside the source frame and never zooms past [minZoom] of the
     * source's short side.
     */
    fun cropWindow(
        subjects: List<SubjectBox>,
        sourceAspect: Float,
        targetAspect: Float,
        minZoom: Float = 0.3f
    ): NormRect {
        require(sourceAspect > 0f && targetAspect > 0f) { "aspect ratios must be positive" }
        val ratio = targetAspect / sourceAspect
        val width: Float
        val height: Float
        if (ratio <= 1f) {
            width = ratio.coerceAtLeast(minZoom)
            height = 1f
        } else {
            width = 1f
            height = (1f / ratio).coerceAtLeast(minZoom)
        }

        val trusted = subjects.filter { it.confidence >= 0.3f }
        val subjectCenterX = if (trusted.isEmpty()) 0.5f else trusted.map { it.rect.centerX }.average().toFloat()
        val subjectCenterY = if (trusted.isEmpty()) 0.5f else trusted.map { it.rect.centerY }.average().toFloat()

        val left = (subjectCenterX - width / 2f).coerceIn(0f, (1f - width).coerceAtLeast(0f))
        val top = (subjectCenterY - height / 2f).coerceIn(0f, (1f - height).coerceAtLeast(0f))
        return NormRect(left, top, left + width, top + height)
    }

    /** Exponential smoothing; [smoothing] 1 = jump straight to the target. */
    fun smooth(previous: NormRect, next: NormRect, smoothing: Float): NormRect {
        val t = smoothing.coerceIn(0f, 1f)
        fun mix(a: Float, b: Float) = a + (b - a) * t
        return NormRect(
            mix(previous.left, next.left),
            mix(previous.top, next.top),
            mix(previous.right, next.right),
            mix(previous.bottom, next.bottom)
        ).clampedToUnit()
    }

    /** Interpolates a slow push-in between two crop windows. */
    fun kenBurns(
        start: NormRect,
        end: NormRect,
        progress: Float,
        curve: EasingCurve = EasingCurve.EASE_IN_OUT
    ): NormRect {
        val t = TransitionMath.ease(curve, progress.coerceIn(0f, 1f))
        return NormRect(
            start.left + (end.left - start.left) * t,
            start.top + (end.top - start.top) * t,
            start.right + (end.right - start.right) * t,
            start.bottom + (end.bottom - start.bottom) * t
        ).clampedToUnit()
    }

    /** Pixel crop for a source of [sourceWidth] x [sourceHeight]. */
    fun toPixels(rect: NormRect, sourceWidth: Int, sourceHeight: Int): CropRect {
        val safe = rect.clampedToUnit()
        val x = (safe.left * sourceWidth).toInt().coerceIn(0, max(0, sourceWidth - 2))
        val y = (safe.top * sourceHeight).toInt().coerceIn(0, max(0, sourceHeight - 2))
        var width = (safe.width * sourceWidth).toInt().coerceAtLeast(2)
        var height = (safe.height * sourceHeight).toInt().coerceAtLeast(2)
        width = min(width, sourceWidth - x)
        height = min(height, sourceHeight - y)
        return CropRect(x, y, width - (width % 2), height - (height % 2))
    }

    /** Union of the subject boxes, padded by [padding] of the frame. */
    fun subjectUnion(subjects: List<SubjectBox>, padding: Float = 0.08f): NormRect {
        val union = NormRect.union(subjects.map { it.rect })
        return NormRect(
            (union.left - padding).coerceIn(0f, 1f),
            (union.top - padding).coerceIn(0f, 1f),
            (union.right + padding).coerceIn(0f, 1f),
            (union.bottom + padding).coerceIn(0f, 1f)
        )
    }

    /** True when the subject leaves the crop and the window must be re-planned. */
    fun needsRecrop(window: NormRect, subject: NormRect, tolerance: Float = 0.02f): Boolean {
        val inside = subject.left >= window.left - tolerance &&
            subject.right <= window.right + tolerance &&
            subject.top >= window.top - tolerance &&
            subject.bottom <= window.bottom + tolerance
        return !inside
    }

    /** Static tripod crop, used for photos without a detector. */
    fun centerCrop(sourceAspect: Float, targetAspect: Float): NormRect =
        cropWindow(emptyList(), sourceAspect, targetAspect)
}
