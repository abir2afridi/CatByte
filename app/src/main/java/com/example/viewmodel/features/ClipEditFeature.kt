package com.example.viewmodel.features

import com.example.engine.ClipSplit
import com.example.engine.TimelineEdits
import com.example.viewmodel.EditorViewModel
import java.util.UUID

/** Structural clip edits: split, delete, duplicate, freeze frame and reverse. */
internal object ClipEditFeature {

    /**
     * Splits the clip under the playhead and keeps the playhead exactly on the
     * cut. Returns false when the playhead is outside a clip or too close to an
     * edge, so the UI can tell the user instead of silently doing nothing.
     *
     * A selected overlay is split first (it is on top of the main track); then
     * the main-track clip under the playhead. Playback is paused before editing
     * so the cut lands on the frame the user is looking at.
     */
    fun splitClipAtPlayhead(vm: EditorViewModel): Boolean {
        val project = vm._currentProject.value ?: return false
        if (vm._isPlaying.value) {
            vm.pausePlayback()
            vm.playerManager?.syncPosition()
            vm.playerManager?.currentPositionMs?.value?.let { vm._currentPositionMs.value = it }
        }
        val playhead = vm._currentPositionMs.value
        val selectedId = vm._selectedClipId.value
        val min = ClipSplit.MIN_SPLIT_MS

        if (selectedId != null && project.isTrackLockedForClip(selectedId)) return false
        val selectedIsMain = selectedId != null && project.clips.any { it.id == selectedId }
        if (project.isMainTrackLocked && (selectedId == null || selectedIsMain)) return false

        if (selectedId != null) {
            project.overlayTracks.forEachIndexed { trackIndex, track ->
                val clip = track.clips.firstOrNull { it.id == selectedId } ?: return@forEachIndexed
                val localMs = playhead - clip.startTimelineMs
                if (localMs < min || localMs > clip.durationMs - min) return@forEachIndexed

                val split = ClipSplit.split(clip, localMs) { UUID.randomUUID().toString() }
                    ?: return@forEachIndexed
                val second = split.second.copy(
                    startTimelineMs = clip.startTimelineMs + localMs
                )
                val newClips = track.clips.flatMap { existing ->
                    if (existing.id == clip.id) listOf(split.first, second) else listOf(existing)
                }
                vm.updateProject { current ->
                    current.copy(
                        overlayTracks = current.overlayTracks.mapIndexed { index, currentTrack ->
                            if (index == trackIndex) currentTrack.copy(clips = newClips) else currentTrack
                        }
                    )
                }
                vm._selectedClipId.value = second.id
                return true
            }
        }

        var cursorMs = 0L
        project.clips.forEachIndexed { index, clip ->
            val localMs = playhead - cursorMs
            if (localMs in min..(clip.durationMs - min)) {
                val split = ClipSplit.split(clip, localMs) { UUID.randomUUID().toString() }
                    ?: return false
                val first = split.first.copy(
                    startTimelineMs = clip.startTimelineMs
                )
                val second = split.second.copy(
                    startTimelineMs = clip.startTimelineMs + localMs
                )
                val clips = project.clips.toMutableList()
                clips.removeAt(index)
                clips.add(index, second)
                clips.add(index, first)
                vm.updateProject { it.copy(clips = clips) }
                vm._selectedClipId.value = second.id
                return true
            }
            cursorMs += clip.durationMs
        }
        return false
    }

    /**
     * Deletes the selected clip through [TimelineEdits.delete]: the main track
     * ripples (the gap closes, list order defines time) while overlay tracks
     * lift (absolute positions of the remaining overlays stay put).
     */
    fun deleteSelectedClip(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            if (proj.isTrackLockedForClip(clipId)) return@updateProject proj
            val mainSpans = proj.clips.map {
                TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs)
            }
            val byId = proj.clips.associateBy { it.id }
            val newMainClips = TimelineEdits.delete(mainSpans, clipId, ripple = true)
                .mapNotNull { span -> byId[span.id] }

            val newOverlays = proj.overlayTracks.map { track ->
                if (track.clips.none { it.id == clipId }) {
                    track
                } else {
                    val spans = track.clips.map {
                        TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs)
                    }
                    val lookup = track.clips.associateBy { it.id }
                    track.copy(
                        clips = TimelineEdits.delete(spans, clipId, ripple = false)
                            .mapNotNull { span -> lookup[span.id] }
                    )
                }
            }
            proj.copy(clips = newMainClips, overlayTracks = newOverlays)
        }
        val updated = vm._currentProject.value
        vm._selectedClipId.value = updated?.clips?.firstOrNull()?.id
            ?: updated?.overlayTracks?.firstOrNull()?.clips?.firstOrNull()?.id
    }

    fun duplicateSelectedClip(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        var duplicatedOverlayId: String? = null
        vm.updateProject { proj ->
            val index = proj.clips.indexOfFirst { it.id == clipId }
            if (index != -1) {
                val original = proj.clips[index]
                val copy = original.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${original.title} (Copy)"
                )
                val newClips = proj.clips.toMutableList()
                newClips.add(index + 1, copy)
                proj.copy(clips = newClips)
            } else {
                // Overlay clips are absolute on the timeline: place the copy
                // immediately after the original instead of stacking it on top.
                val updatedTracks = proj.overlayTracks.map { track ->
                    val overlayIndex = track.clips.indexOfFirst { it.id == clipId }
                    if (overlayIndex == -1) {
                        track
                    } else {
                        val original = track.clips[overlayIndex]
                        val copy = original.copy(
                            id = UUID.randomUUID().toString(),
                            title = "${original.title} (Copy)",
                            startTimelineMs = original.startTimelineMs + original.durationMs
                        )
                        duplicatedOverlayId = copy.id
                        track.copy(clips = track.clips + copy)
                    }
                }
                proj.copy(overlayTracks = updatedTracks)
            }
        }
        duplicatedOverlayId?.let { vm._selectedClipId.value = it }
    }

    /**
     * Inserts a 3-second freeze frame at the current playhead position.
     * Splits the active clip and creates a held frame with preserved styling.
     */
    fun freezeFrameAtPlayhead(vm: EditorViewModel) {
        val project = vm._currentProject.value ?: return
        val playhead = vm._currentPositionMs.value
        val clips = project.clips.toMutableList()

        var accumulated = 0L
        var targetIndex = -1
        for (i in clips.indices) {
            val clipEnd = accumulated + clips[i].durationMs
            if (playhead in accumulated..clipEnd) {
                targetIndex = i
                break
            }
            accumulated = clipEnd
        }

        if (targetIndex != -1) {
            val target = clips[targetIndex]
            val splitOffsetMs = (playhead - accumulated).coerceIn(0L, target.durationMs)
            val remainingMs = target.durationMs - splitOffsetMs

            val firstHalf = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = splitOffsetMs.coerceAtLeast(100L),
                trimEndMs = target.trimStartMs + splitOffsetMs,
                title = target.title
            )
            val freezeClip = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = 3000L,
                trimStartMs = target.trimStartMs + splitOffsetMs,
                trimEndMs = target.trimStartMs + splitOffsetMs + 33L,
                speed = 0.011f,
                title = "${target.title} (Freeze)"
            )
            val secondHalf = target.copy(
                id = UUID.randomUUID().toString(),
                durationMs = remainingMs.coerceAtLeast(100L),
                trimStartMs = target.trimStartMs + splitOffsetMs,
                title = target.title
            )

            clips.removeAt(targetIndex)
            if (remainingMs > 100L) {
                clips.add(targetIndex, secondHalf)
            }
            clips.add(targetIndex, freezeClip)
            if (splitOffsetMs > 100L) {
                clips.add(targetIndex, firstHalf)
            }

            vm.updateProject("freeze_${target.id}") { it.copy(clips = clips) }
            vm._selectedClipId.value = freezeClip.id
        }
    }

    /** Toggles reverse playback on the selected clip. */
    fun reverseSelectedClip(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.copy(
                clips = proj.clips.map { clip ->
                    if (clip.id == clipId) {
                        val rev = !clip.isReversed
                        clip.copy(
                            isReversed = rev,
                            title = if (rev) "${clip.title} (Reverse)" else clip.title.replace(" (Reverse)", "")
                        )
                    } else clip
                },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(
                        clips = track.clips.map { clip ->
                            if (clip.id == clipId) {
                                val rev = !clip.isReversed
                                clip.copy(
                                    isReversed = rev,
                                    title = if (rev) "${clip.title} (Reverse)" else clip.title.replace(" (Reverse)", "")
                                )
                            } else clip
                        }
                    )
                }
            )
        }
    }
}
