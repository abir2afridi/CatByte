package com.example.data

/**
 * Guards preview playback against two Media3 CompositionPlayer 1.11.1 defects.
 * Own file on purpose: these are tuned positions/thresholds that must not be
 * diluted by unrelated edits to [PlayerManager] or `EditorViewModel`.
 *
 * 1. **Last-frame freeze** (androidx/media#3452): when playback *starts* on the
 *    last frame of an outgoing sequence item, CompositionPlayer renders the
 *    next item's first frame, advances ~100-150 ms, then freezes for good.
 *    It reproduces through "seek while paused, then play()" — exactly what the
 *    fixed-centre timeline does: scrubbing while paused parks the playhead on
 *    a clip's final frame, then Play is pressed. The tested workaround is to
 *    start on the item boundary instead of inside the last frames before it
 *    (0 of 316 probes froze).
 *
 * 2. **Same-position seek silence** (androidx/media#3482): seeking to the
 *    position the player already reports, while paused, flushes the audio
 *    graph and the following play() starts silent. Playback start therefore
 *    must never re-seek to the position a scrub already applied.
 *
 * 3. **Stuck seek** (device log): after a scrub seek of more than ~100 ms the
 *    player parks in STATE_BUFFERING forever, and setComposition() on the same
 *    instance never recovers it. The only proven recovery is a brand-new
 *    player instance ([PlayerManager.rebuildPreviewAt]); nudges are useless
 *    because they never render a frame.
 */
internal object PlaybackStartPolicy {

    /**
     * Output frames before a cut treated as the unsafe "last frame" zone.
     * Two frames covers the 30 fps last frame plus the frame the seek snaps
     * to, and the 60 fps / 2x case from the Media3 report (~16-25 ms output).
     */
    const val GUARD_FRAMES = 2

    /**
     * Time window before a clip cut that must not be used as a playback start.
     *
     * androidx/media#3452 freezes ~130-150 ms after a start that sits on the
     * outgoing item's last frame — i.e. the freeze lands ~100 ms AFTER the cut.
     * The safe position is the cut itself (0 of 316 probes froze starting on a
     * boundary), so any start inside this window before a cut is moved onto the
     * cut. Wider than [GUARD_FRAMES] frames on purpose: the defect is driven by
     * how close the pre-cut seek left the audio renderer to the transition, not
     * by the exact frame.
     */
    const val TRANSITION_WINDOW_MS = 150L

    /** Duration of one output frame; invalid fps is clamped to a sane range. */
    fun frameDurationMs(fps: Int): Long = 1_000L / fps.coerceIn(1, 120)

    /**
     * Timeline positions where the primary composition sequence changes item:
     * cumulative main-track clip durations. The main track is magnetic, so its
     * clips are laid out back to back in both the editor and the composition.
     */
    fun mainClipBoundariesMs(clipDurationsMs: List<Long>): List<Long> {
        var acc = 0L
        return clipDurationsMs.map { duration ->
            acc += duration.coerceAtLeast(0L)
            acc
        }
    }

    /**
     * Returns [positionMs] unless starting playback there is known to freeze
     * CompositionPlayer.
     *
     * - Inside [TRANSITION_WINDOW_MS] (at least two frames) before an internal
     *   clip boundary -> return that boundary (the next clip's first frame),
     *   the tested-safe start.
     * - Inside the same window at the very end of the timeline -> return 0 so
     *   Play restarts instead of starting on the final frame (which would look
     *   frozen / immediately end).
     *
     * A position exactly on a boundary is already safe and is returned as is.
     */
    fun safeStartMs(
        positionMs: Long,
        durationMs: Long,
        fps: Int,
        boundariesMs: List<Long>
    ): Long {
        val duration = durationMs.coerceAtLeast(0L)
        val position = positionMs.coerceIn(0L, duration)
        val guardMs = maxOf(
            frameDurationMs(fps) * GUARD_FRAMES,
            TRANSITION_WINDOW_MS
        )
        if (duration <= 0L || guardMs <= 0L) return position

        // The final frame of the whole timeline: restart from the top.
        if (position >= duration - guardMs) return 0L

        val boundary = boundariesMs.firstOrNull { boundary ->
            boundary > position &&
                boundary < duration &&
                boundary - position <= guardMs
        }
        return boundary ?: position
    }
}
