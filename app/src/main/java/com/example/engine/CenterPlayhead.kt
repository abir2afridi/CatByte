package com.example.engine

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Fixed center playhead (CapCut timeline behaviour).
 *
 * The playhead never moves on screen: the timeline content scrolls underneath
 * it. These helpers convert between a frame and a horizontal content offset so
 * the gesture layer, the ruler and the clip strips all agree.
 */
object CenterPlayhead {

    fun maxOffsetPx(contentWidthPx: Float, viewportWidthPx: Float): Float =
        max(0f, contentWidthPx - viewportWidthPx)

    fun clampOffset(offsetPx: Float, contentWidthPx: Float, viewportWidthPx: Float): Float =
        offsetPx.coerceIn(0f, maxOffsetPx(contentWidthPx, viewportWidthPx))

    /** Content offset that puts [playheadFrame] exactly under the playhead. */
    fun offsetForFrame(
        playheadFrame: Long,
        pxPerFrame: Float,
        viewportWidthPx: Float,
        contentWidthPx: Float
    ): Float {
        if (pxPerFrame <= 0f) return 0f
        val raw = playheadFrame * pxPerFrame - viewportWidthPx / 2f
        return clampOffset(raw, contentWidthPx, viewportWidthPx)
    }

    /** Frame currently visible under the fixed playhead for a given offset. */
    fun frameAtPlayhead(
        offsetPx: Float,
        pxPerFrame: Float,
        viewportWidthPx: Float
    ): Long {
        if (pxPerFrame <= 0f) return 0L
        val frame = (offsetPx + viewportWidthPx / 2f) / pxPerFrame
        return frame.roundToLong().coerceAtLeast(0L)
    }

    /**
     * Offset to keep after a pinch zoom, so the content under the playhead stays
     * put while the scale changes.
     */
    fun offsetAfterZoom(
        playheadFrame: Long,
        newPxPerFrame: Float,
        viewportWidthPx: Float,
        contentWidthPx: Float
    ): Float = offsetForFrame(playheadFrame, newPxPerFrame, viewportWidthPx, contentWidthPx)

    /**
     * True when the playhead is pinned at the very start/end of the timeline and
     * therefore cannot stay centered; the UI uses this to show the end padding.
     */
    fun pinnedAtEdges(
        playheadFrame: Long,
        pxPerFrame: Float,
        viewportWidthPx: Float,
        contentWidthPx: Float
    ): Boolean {
        if (pxPerFrame <= 0f) return true
        val ideal = playheadFrame * pxPerFrame - viewportWidthPx / 2f
        return ideal < 0f || ideal > maxOffsetPx(contentWidthPx, viewportWidthPx)
    }

    /**
     * Horizontal offset for drawing a clip edge when the content is scrolled by
     * [offsetPx].
     */
    fun clipDrawOffsetPx(
        clipStartFrame: Long,
        pxPerFrame: Float,
        offsetPx: Float
    ): Float = clipStartFrame * pxPerFrame - offsetPx
}
