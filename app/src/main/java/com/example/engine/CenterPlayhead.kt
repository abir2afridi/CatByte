package com.example.engine

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Fixed centre playhead (CapCut timeline behaviour).
 *
 * The needle never moves on screen: it is pinned to the horizontal centre of
 * the scrolling viewport and the timeline content scrolls underneath it.
 *
 * To make that possible at time 0 AND at the very end, the scroll content is
 * padded with HALF A VIEWPORT on both sides. Those pads are exactly why the
 * scroll offset for a frame is simply `frame * pxPerFrame` — the leading pad
 * already puts time 0 at the centre, so no extra `viewport / 2` term is needed.
 *
 * Own file on purpose: the ruler, the scroll observer and the follow-scroll
 * all derive their positions from these rules, so a future edit elsewhere
 * cannot silently make the needle drift again.
 */
object CenterPlayhead {

    /** Empty lead-in / lead-out on each side of the timeline, in pixels. */
    fun edgePaddingPx(viewportWidthPx: Float): Float =
        viewportWidthPx.coerceAtLeast(0f) / 2f

    /** Width of the scroll content: timeline plus one half-viewport pad per side. */
    fun paddedContentWidthPx(timelineWidthPx: Float, viewportWidthPx: Float): Float =
        timelineWidthPx.coerceAtLeast(0f) + 2f * edgePaddingPx(viewportWidthPx)

    /**
     * Largest scroll offset. The two half-viewport pads cancel the viewport
     * exactly, so this equals the timeline width.
     */
    fun maxOffsetPx(timelineWidthPx: Float, viewportWidthPx: Float): Float =
        max(
            0f,
            paddedContentWidthPx(timelineWidthPx, viewportWidthPx) -
                viewportWidthPx.coerceAtLeast(0f)
        )

    /** Content offset that puts [playheadFrame] exactly under the fixed needle. */
    fun offsetForFrame(
        playheadFrame: Long,
        pxPerFrame: Float,
        timelineWidthPx: Float,
        viewportWidthPx: Float
    ): Float {
        if (pxPerFrame <= 0f) return 0f
        val raw = playheadFrame.coerceAtLeast(0L) * pxPerFrame
        return raw.coerceIn(0f, maxOffsetPx(timelineWidthPx, viewportWidthPx))
    }

    /** Frame currently under the fixed needle for a given scroll offset. */
    fun frameAtPlayhead(offsetPx: Float, pxPerFrame: Float): Long {
        if (pxPerFrame <= 0f) return 0L
        return (offsetPx.coerceAtLeast(0f) / pxPerFrame).roundToLong()
    }
}
