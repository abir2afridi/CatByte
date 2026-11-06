package com.example.engine

import kotlin.math.roundToLong

/**
 * Pure geometry shared by every timeline gesture.
 *
 * The timeline is a horizontally scrollable canvas whose width is derived from
 * duration and zoom. Keeping the conversion here (instead of scattering
 * `ms * msToDp` across composables) means the ruler, the playhead drag and the
 * follow-scroll can never disagree about where a millisecond lives on screen.
 */
object TimelineGeometry {

    /** Default horizontal scale at zoom 1.0. */
    const val DP_PER_SECOND = 65f

    /** Empty timeline still needs enough room to drop the first clip. */
    const val MIN_TIMELINE_MS = 4_000L

    /** Extra room after the last frame so the playhead can reach the end. */
    const val CONTENT_PADDING_DP = 260f

    /** Magnet radius for ruler taps and playhead drags, in dp. */
    const val SNAP_THRESHOLD_DP = 12f

    /** Half of the playhead's touch target. */
    const val PLAYHEAD_HIT_HALF_DP = 22f

    /** Height of one overlay track row; drag maths uses it for cross-track drops. */
    const val OVERLAY_TRACK_HEIGHT_DP = 46f

    private const val MIN_ZOOM = 0.25f
    private const val MAX_ZOOM = 8f

    fun dpPerMs(zoom: Float): Float =
        (DP_PER_SECOND * zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)) / 1_000f

    fun contentWidthDp(durationMs: Long, zoom: Float): Float =
        durationMs.coerceAtLeast(MIN_TIMELINE_MS) * dpPerMs(zoom) + CONTENT_PADDING_DP

    fun contentXDp(positionMs: Long, zoom: Float): Float =
        positionMs.coerceAtLeast(0L) * dpPerMs(zoom)

    fun msAtContentXDp(xDp: Float, zoom: Float): Long =
        (xDp / dpPerMs(zoom)).roundToLong().coerceAtLeast(0L)

    /**
     * New timeline position after dragging [dragPx] from [anchorMs].
     *
     * Drag deltas are accumulated by the gesture layer instead of reading the
     * playhead's own moving position, which is what made the old cursor stick
     * and jump while the thumb travelled.
     */
    fun positionMsAfterDrag(
        anchorMs: Long,
        dragPx: Float,
        density: Float,
        zoom: Float
    ): Long {
        if (density <= 0f) return anchorMs.coerceAtLeast(0L)
        val dragDp = dragPx / density
        return (anchorMs + dragDp / dpPerMs(zoom)).roundToLong().coerceAtLeast(0L)
    }

    /** Screen-space x of a timeline position, given the current scroll offset. */
    fun screenXDp(
        positionMs: Long,
        zoom: Float,
        scrollPx: Int,
        density: Float
    ): Float {
        if (density <= 0f) return contentXDp(positionMs, zoom)
        return contentXDp(positionMs, zoom) - scrollPx / density
    }
}
