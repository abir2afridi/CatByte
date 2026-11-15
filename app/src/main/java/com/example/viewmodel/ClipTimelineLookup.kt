package com.example.viewmodel

import com.example.model.MediaClip
import com.example.model.VideoProject

/** Applies [transform] to a clip on the main track or any overlay track. */
internal fun VideoProject.withUpdatedClip(
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

/**
 * Clip-local time. Main-track clips are laid out cumulatively (their stored
 * startTimelineMs is not authoritative), overlay clips use their explicit
 * startTimelineMs. Keyframes are always stored in clip-local time.
 */
internal fun VideoProject.localTimeForClip(clipId: String, timelineMs: Long): Long? {
    var cursor = 0L
    for (clip in clips) {
        if (clip.id == clipId) {
            return (timelineMs - cursor).coerceIn(0L, clip.durationMs)
        }
        cursor += clip.durationMs
    }
    overlayTracks.forEach { track ->
        track.clips.firstOrNull { it.id == clipId }?.let { clip ->
            return (timelineMs - clip.startTimelineMs).coerceIn(0L, clip.durationMs)
        }
    }
    return null
}
