package com.example.viewmodel.features

import com.example.engine.SnapEngine
import com.example.viewmodel.EditorViewModel

/** Moving clips around the timeline: reorder, slide and cross-track drops. */
internal object ClipMoveFeature {

    /** Moves a clip to a new position in the timeline (drag-reorder). */
    fun moveClip(vm: EditorViewModel, fromIndex: Int, toIndex: Int) {
        val project = vm._currentProject.value ?: return
        if (project.isMainTrackLocked) return
        val clips = project.clips.toMutableList()
        if (fromIndex !in clips.indices || toIndex !in clips.indices) return
        val clip = clips.removeAt(fromIndex)
        clips.add(toIndex, clip)
        vm.updateProject { it.copy(clips = clips) }
    }

    /**
     * Moves an overlay clip to a new start time. The drop snaps to neighbouring
     * overlay edges ([SnapEngine.magneticStart]) and clamps inside the project,
     * so a fling can never park a clip outside the timeline.
     */
    fun moveOverlayClip(vm: EditorViewModel, clipId: String, desiredStartMs: Long) {
        vm.updateProject { proj ->
            if (proj.isTrackLockedForClip(clipId)) return@updateProject proj
            val trackIndex = proj.overlayTracks.indexOfFirst { track ->
                track.clips.any { it.id == clipId }
            }
            if (trackIndex == -1) return@updateProject proj
            val track = proj.overlayTracks[trackIndex]
            val clip = track.clips.first { it.id == clipId }
            val occupied = SnapEngine.occupiedRanges(
                track.clips
                    .filter { it.id != clipId }
                    .map { it.startTimelineMs to it.durationMs }
            )
            val snapped = SnapEngine.magneticStart(desiredStartMs, clip.durationMs, occupied)
            val maxStart = (proj.totalDurationMs - clip.durationMs).coerceAtLeast(0L)
            val start = snapped.coerceIn(0L, maxStart)
            val updatedTrack = track.copy(
                clips = track.clips.map { existing ->
                    if (existing.id == clipId) existing.copy(startTimelineMs = start) else existing
                }
            )
            proj.copy(
                overlayTracks = proj.overlayTracks.mapIndexed { index, current ->
                    if (index == trackIndex) updatedTrack else current
                }
            )
        }
    }

    /**
     * Moves a clip to another overlay track at the requested start time.
     *
     * Cross-track drops are the CapCut "free overlay" model: the target track is
     * chosen by the vertical part of the drag, then [SnapEngine] resolves the
     * exact start inside that track. Locked source/target tracks refuse the move.
     */
    fun moveClipToTrack(vm: EditorViewModel, clipId: String, targetTrackId: String, desiredStartMs: Long) {
        vm.updateProject { proj ->
            if (proj.isTrackLocked(targetTrackId) || proj.trackIdForClip(clipId) == targetTrackId) {
                return@updateProject proj
            }
            if (proj.isTrackLockedForClip(clipId)) return@updateProject proj
            val sourceIndex = proj.overlayTracks.indexOfFirst { track ->
                track.clips.any { it.id == clipId }
            }
            val targetIndex = proj.overlayTracks.indexOfFirst { it.id == targetTrackId }
            if (sourceIndex == -1 || targetIndex == -1) return@updateProject proj

            val clip = proj.overlayTracks[sourceIndex].clips.first { it.id == clipId }
            val occupied = SnapEngine.occupiedRanges(
                proj.overlayTracks[targetIndex].clips.map { it.startTimelineMs to it.durationMs }
            )
            val snapped = SnapEngine.magneticStart(desiredStartMs, clip.durationMs, occupied)
            val maxStart = (proj.totalDurationMs - clip.durationMs).coerceAtLeast(0L)
            val start = snapped.coerceIn(0L, maxStart)
            val moved = clip.copy(startTimelineMs = start)

            proj.copy(
                overlayTracks = proj.overlayTracks.mapIndexed { index, track ->
                    when (index) {
                        sourceIndex -> track.copy(clips = track.clips.filterNot { it.id == clipId })
                        targetIndex -> track.copy(clips = track.clips + moved)
                        else -> track
                    }
                }
            )
        }
    }
}
