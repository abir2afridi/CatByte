package com.example.engine

/**
 * Geometry of the editing guides drawn over the preview.
 *
 * Broadcast/editor convention:
 *  - **Action safe** (90%): everything important should stay inside this box.
 *  - **Title safe** (80%): text should stay inside this box, because TV
 *    overscan and phone notches can crop the outer edge.
 *  - **Rule of thirds**: two horizontal and two vertical lines at 1/3 and 2/3.
 *
 * The guides are preview-only: they are never burned into an export.
 */
object SafeAreaMath {

    /** Fraction of the frame kept inside the action-safe guides. */
    const val ACTION_SAFE = 0.90f

    /** Fraction of the frame kept inside the title-safe guides. */
    const val TITLE_SAFE = 0.80f

    /** Positions of the rule-of-thirds lines along an axis. */
    val THIRDS: List<Float> = listOf(1f / 3f, 2f / 3f)

    /** A rectangle in pixel space. */
    data class GuideRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }

    /**
     * Centred guide box for [scale] (0..1) inside a [width] × [height] frame.
     * Scales outside the range are clamped so the guides can never invert.
     */
    fun guideRect(scale: Float, width: Float, height: Float): GuideRect {
        val safeWidth = width.coerceAtLeast(0f)
        val safeHeight = height.coerceAtLeast(0f)
        val fraction = scale.coerceIn(0f, 1f)
        val insetX = safeWidth * (1f - fraction) / 2f
        val insetY = safeHeight * (1f - fraction) / 2f
        return GuideRect(
            left = insetX,
            top = insetY,
            right = safeWidth - insetX,
            bottom = safeHeight - insetY
        )
    }
}
