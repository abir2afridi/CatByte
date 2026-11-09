package com.example.engine

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Fixed centre playhead (CapCut timeline behaviour).
 *
 * The needle never moves on screen: it is pinned to the horizontal centre of
 * the whole timeline panel (so it lines up with the play button above it) and
 * the timeline content scrolls underneath it.
 *
 * The viewport starts to the right of the fixed left gutter, so the needle is
 * NOT at `viewport / 2`; its inset inside the viewport is
 * `(viewportWidth - gutterWidth) / 2`. The scroll content is padded with that
 * inset on the leading side and the remainder of the viewport on the trailing
 * side. Those pads are exactly why the scroll offset for a frame is simply
 * `frame * pxPerFrame` — the leading pad already puts time 0 under the needle,
 * so no extra term is needed.
 *
 * Own file on purpose: the ruler, the scroll observer and the follow-scroll
 * all derive their positions from these rules, so a future edit elsewhere
 * cannot silently make the needle drift again.
 */
object CenterPlayhead {

    /**
     * X of the fixed needle inside the scrolling viewport.
     *
     * The panel is `gutter + viewport` wide and the needle sits at the panel
     * centre, i.e. `gutter + (viewport - gutter) / 2` from the panel's left
     * edge — which is `(viewport - gutter) / 2` inside the viewport.
     */
    fun needleInsetPx(viewportWidthPx: Float, gutterWidthPx: Float): Float =
        ((viewportWidthPx.coerceAtLeast(0f) - gutterWidthPx.coerceAtLeast(0f)) / 2f)
            .coerceAtLeast(0f)

    /** Empty lead-in before time 0; equals the needle inset so time 0 is centred. */
    fun leadingPaddingPx(needleInsetPx: Float): Float =
        needleInsetPx.coerceAtLeast(0f)

    /** Empty lead-out after the last frame so the end can reach the needle. */
    fun trailingPaddingPx(viewportWidthPx: Float, needleInsetPx: Float): Float =
        (viewportWidthPx.coerceAtLeast(0f) - needleInsetPx.coerceAtLeast(0f))
            .coerceAtLeast(0f)

    /**
     * Width of the scroll content: timeline plus the lead-in and lead-out pads.
     * The two pads always add up to exactly one viewport, so this is
     * `timelineWidth + viewportWidth` wherever the needle sits.
     */
    fun paddedContentWidthPx(timelineWidthPx: Float, viewportWidthPx: Float): Float =
        timelineWidthPx.coerceAtLeast(0f) + viewportWidthPx.coerceAtLeast(0f)

    /** Largest scroll offset: the pads cancel the viewport exactly. */
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
