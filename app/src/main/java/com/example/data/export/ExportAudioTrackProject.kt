package com.example.data.export

import com.example.model.VideoProject

/**
 * The project that is rendered when the user exports one audio track on its
 * own.
 *
 * Everything but the chosen track is dropped and the track starts at zero, so
 * the file holds the track itself instead of a long silence with the track at
 * the end. The track keeps its own volume, fades, speed and effects, so the
 * exported file sounds like the track in the mixer.
 */
internal fun singleTrackProject(project: VideoProject, audioId: String): VideoProject? {
    val clip = project.audioTracks.firstOrNull { it.id == audioId } ?: return null
    return project.copy(
        clips = emptyList(),
        overlayTracks = emptyList(),
        textOverlays = emptyList(),
        stickers = emptyList(),
        strokes = emptyList(),
        effects = emptyList(),
        audioTracks = listOf(clip.copy(startTimelineMs = 0L))
    )
}
