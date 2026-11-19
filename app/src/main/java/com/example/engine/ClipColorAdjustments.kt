package com.example.engine

import com.example.model.MediaClip
import com.example.model.VideoProject

/**
 * The colour grade of a clip and the "original" it can be compared with.
 *
 * The Adjust panel edits the legacy grade sliders (brightness, contrast,
 * saturation, vignette) and the per-channel HSL adjustments. Both live on the
 * clip and are applied by the shared composition, so a preview that drops them
 * really shows the untouched source - which is what the "Before / After"
 * comparison needs.
 */
object ClipColorAdjustments {

    /** The neutral values of every colour control of a clip. */
    fun neutralized(clip: MediaClip): MediaClip = clip.copy(
        brightness = 0f,
        contrast = 1f,
        saturation = 1f,
        vignette = 0f,
        hslAdjustments = emptyList()
    )

    /**
     * The project as it looks without [clipId]'s colour adjustments.
     *
     * The project itself is never modified: this is a preview-only variant,
     * so the exported file always contains the real grade.
     */
    fun withoutAdjustments(project: VideoProject, clipId: String?): VideoProject {
        if (clipId.isNullOrBlank()) return project
        var changed = false
        val updatedMain = project.clips.map { clip ->
            if (clip.id == clipId) {
                changed = true
                neutralized(clip)
            } else {
                clip
            }
        }
        val updatedOverlays = project.overlayTracks.map { track ->
            val updatedClips = track.clips.map { clip ->
                if (clip.id == clipId) {
                    changed = true
                    neutralized(clip)
                } else {
                    clip
                }
            }
            if (updatedClips == track.clips) track else track.copy(clips = updatedClips)
        }
        return if (changed) {
            project.copy(clips = updatedMain, overlayTracks = updatedOverlays)
        } else {
            project
        }
    }
}
