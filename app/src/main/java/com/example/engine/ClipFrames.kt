package com.example.engine

import com.example.model.MediaClip

/**
 * Filmstrip frame sampling rule, as pure maths.
 *
 * Own file on purpose: the timeline's filmstrip must show REAL, DIFFERENT
 * frames of the imported video across the clip's selected source window
 * (in-point .. out-point) — not the same first frame repeated. Own file + unit
 * tests so future edits to the timeline UI cannot quietly break it.
 *
 * Two tuned details:
 *  - samples sit at the CENTRE of each tile's time span, so the strip is even;
 *  - samples snap to a fixed [SAMPLE_GRID_MS] grid so zooming in/out reuses
 *    Coil's cached frames instead of re-decoding a whole new set (keeps the
 *    timeline smooth).
 */
object ClipFrames {

    /** Snap grid for frame samples, in milliseconds. */
    const val SAMPLE_GRID_MS = 500L

    /**
     * Source offset (in ms from the clip's trim start) for filmstrip tile
     * [index] of [count], inside a source window of [sourceWindowMs].
     * Always within `0 until sourceWindowMs` so the retriever never seeks past
     * the end of the media.
     */
    fun sampleOffsetMs(sourceWindowMs: Long, index: Int, count: Int): Long {
        if (count <= 0 || sourceWindowMs <= 0) return 0L
        val raw = (((index + 0.5) * sourceWindowMs) / count).toLong()
            .coerceIn(0L, sourceWindowMs - 1)
        if (sourceWindowMs <= SAMPLE_GRID_MS * 2) return raw
        val snapped = (raw / SAMPLE_GRID_MS) * SAMPLE_GRID_MS + SAMPLE_GRID_MS / 2
        return snapped.coerceIn(0L, sourceWindowMs - 1)
    }

    /** Convenience: absolute source timestamp for a tile, honouring the trim start. */
    fun sampleSourceMs(clip: MediaClip, index: Int, count: Int): Long =
        clip.trimStartMs.coerceAtLeast(0L) +
            sampleOffsetMs(
                com.example.data.ClipTiming.sourceLengthMs(clip),
                index,
                count
            )
}
