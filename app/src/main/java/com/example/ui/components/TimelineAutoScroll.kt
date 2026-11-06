package com.example.ui.components

import androidx.compose.foundation.ScrollState
import com.example.engine.TimelineGeometry
import kotlin.math.min

/**
 * Edge auto-scroll shared by the playhead needle and the ruler scrub.
 *
 * While dragging past a viewport edge, the timeline nudges itself so a long
 * edit can be scrubbed without lifting the finger.
 */
internal object TimelineAutoScroll {

    /** Distance from a viewport edge where the nudge starts. */
    private const val EDGE_DP = 48f

    /** Largest single nudge, in px. */
    private const val MAX_STEP_DP = 24f

    /**
     * Keeps the timeline position [targetMs] visible while the user drags.
     * Called after every seek so the needle never escapes the viewport.
     */
    fun toReveal(
        targetMs: Long,
        zoom: Float,
        density: Float,
        scrollState: ScrollState
    ) {
        if (density <= 0f) return
        val contentPx = TimelineGeometry.contentXDp(targetMs, zoom) * density
        nudge(contentPx, density, scrollState)
    }

    /**
     * Nudges the scroll when a raw content-space x coordinate (already in px)
     * approaches a viewport edge. Used by the ruler scrub.
     */
    fun atContentPx(
        contentPx: Float,
        density: Float,
        scrollState: ScrollState
    ) {
        nudge(contentPx, density, scrollState)
    }

    private fun nudge(contentPx: Float, density: Float, scrollState: ScrollState) {
        val viewportPx = scrollState.viewportSize
        if (viewportPx <= 0 || density <= 0f) return
        val edgePx = EDGE_DP * density
        val maxStepPx = MAX_STEP_DP * density
        val screenPx = contentPx - scrollState.value
        when {
            screenPx < edgePx && scrollState.value > 0 ->
                scrollState.dispatchRawDelta(-min(edgePx - screenPx, maxStepPx))
            screenPx > viewportPx - edgePx && scrollState.value < scrollState.maxValue ->
                scrollState.dispatchRawDelta(min(screenPx - (viewportPx - edgePx), maxStepPx))
        }
    }
}
