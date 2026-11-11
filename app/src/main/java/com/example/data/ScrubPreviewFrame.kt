package com.example.data

import android.graphics.Bitmap

/**
 * A frame decoded straight from the source media, plus the timeline position it
 * was decoded for.
 *
 * The position travels with the bitmap on purpose. While the overlay is up it is
 * the source of truth for everything drawn in the preview (active clip, text
 * overlay timing), so the canvas does not have to read the live playhead on every
 * scroll tick. That keeps the scroll gesture from recomposing the whole preview
 * at fling rate and guarantees the frame and the overlays always agree.
 */
data class ScrubPreviewFrame(
    val bitmap: Bitmap,
    val positionMs: Long
)
