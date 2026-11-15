package com.example.ui.components

import com.example.engine.SnapCandidate
import com.example.engine.SnapEngine
import com.example.model.VideoProject

/**
 * Frame-accurate snap magnets for clip edges, audio/text/effect edges, beats
 * and markers. The playhead is not a candidate (it is fixed at the needle).
 */
internal fun timelineSnapCandidates(
    project: VideoProject,
    fps: Int,
    beats: List<Long>
): List<SnapCandidate> {
    val edges = mutableListOf<Long>(0L)
    var acc = 0L
    project.clips.forEach { clip ->
        acc += clip.durationMs
        edges += acc * fps / 1_000L
    }
    project.overlayTracks.flatMap { it.clips }.forEach { clip ->
        edges += clip.startTimelineMs * fps / 1_000L
        edges += (clip.startTimelineMs + clip.durationMs) * fps / 1_000L
    }
    project.audioTracks.forEach { audio ->
        edges += audio.startTimelineMs * fps / 1_000L
        edges += (audio.startTimelineMs + audio.durationMs) * fps / 1_000L
    }
    project.textOverlays.forEach { text ->
        edges += text.startTimelineMs * fps / 1_000L
        edges += (text.startTimelineMs + text.durationMs) * fps / 1_000L
    }
    project.effects.forEach { effect ->
        edges += effect.startTimelineMs * fps / 1_000L
        edges += (effect.startTimelineMs + effect.durationMs) * fps / 1_000L
    }
    val beatFrames = beats.map { it * fps / 1_000L }
    val markerFrames = project.markers.map { it.atMs * fps / 1_000L }
    return SnapEngine.collect(
        playhead = -1L,
        clipEdges = edges,
        markers = markerFrames,
        beats = beatFrames
    ).filter { it.frame >= 0L }
}
