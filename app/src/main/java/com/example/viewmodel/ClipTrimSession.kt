package com.example.viewmodel

import com.example.engine.ClipTrimHandle
import com.example.engine.withMagneticMainTrack
import com.example.model.MediaClip
import com.example.model.VideoProject

/**
 * One inline trim-handle drag, snapshotted when the finger goes down.
 *
 * Own file on purpose. This is the tuned CapCut-parity trim behaviour:
 *  - trim changes ONLY the source in/out points (`ClipTrimHandle` maths);
 *  - the main track is magnetic, so a duration change ripples the clips after
 *    it live (left when shorter, right when longer) — this is what makes the
 *    "hidden part" visibly come back after a split instead of hiding under
 *    the next clip;
 *  - overlay clips are absolute: trimming their start edge keeps the end
 *    pinned instead of growing from the wrong side;
 *  - the session is *absolute*: [applyTo] recomputes from [baseClip] using the
 *    cumulative drag distance, so applying it every frame is idempotent;
 *  - one drag = one undo entry (the host pushes history on the first real
 *    change) and one autosave (in the host's `endTrimClip`).
 *
 * Keeping it in a separate file means future edits to `EditorViewModel` or the
 * timeline UI cannot quietly dilute these rules. Pure model code, so the unit
 * tests run on the JVM without Android plumbing.
 */
internal data class ClipTrimSession(
    val clipId: String,
    val edge: ClipTrimHandle.Edge,
    val baseClip: MediaClip,
    /** The project as it was when the finger went down — the undo snapshot. */
    val baseProject: VideoProject
) {
    /** Set after the first real change pushed the single undo entry. */
    var historyPushed: Boolean = false

    /**
     * Applies the drag to [current]. Returns null when nothing changed:
     * sub-frame delta, locked track, or the clip no longer exists.
     */
    fun applyTo(current: VideoProject, cumulativeDeltaMs: Long, fps: Int): VideoProject? {
        if (current.isTrackLockedForClip(clipId)) return null
        val result = ClipTrimHandle.trim(baseClip, edge, cumulativeDeltaMs, fps) ?: return null
        val isOverlay = current.overlayTracks.any { track ->
            track.clips.any { it.id == clipId }
        }
        val updated = current
            .replaceClip(clipId) { clip ->
                val startMs = if (isOverlay && edge == ClipTrimHandle.Edge.START) {
                    (clip.startTimelineMs + (clip.durationMs - result.durationMs)).coerceAtLeast(0L)
                } else {
                    clip.startTimelineMs
                }
                clip.copy(
                    startTimelineMs = startMs,
                    trimStartMs = result.trimStartMs,
                    trimEndMs = result.trimEndMs,
                    durationMs = result.durationMs
                )
            }
            .withMagneticMainTrack()
        return if (updated == current) null else updated
    }
}

/** Replaces one clip on the main track or any overlay track. */
private fun VideoProject.replaceClip(
    clipId: String,
    transform: (MediaClip) -> MediaClip
): VideoProject {
    var changed = false
    val updatedMain = clips.map { clip ->
        if (clip.id == clipId) {
            changed = true
            transform(clip)
        } else clip
    }
    val updatedOverlays = overlayTracks.map { track ->
        val updatedClips = track.clips.map { clip ->
            if (clip.id == clipId) {
                changed = true
                transform(clip)
            } else clip
        }
        if (updatedClips == track.clips) track else track.copy(clips = updatedClips)
    }
    return if (changed) copy(clips = updatedMain, overlayTracks = updatedOverlays) else this
}
