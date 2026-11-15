package com.example.viewmodel.features

import com.example.engine.TimelineEdits
import com.example.model.MediaClip
import com.example.model.VideoTrack
import com.example.viewmodel.EditorViewModel

/** Clip/text selection, multi-selection, overlay clipboard and timeline zoom. */
internal object SelectionFeature {

    fun selectClip(vm: EditorViewModel, clipId: String?) {
        vm._selectedClipId.value = clipId
        vm._selectedClipIds.value = if (clipId == null) emptySet() else setOf(clipId)
        if (clipId != null) {
            vm._selectedTextId.value = null
        }
    }

    /** Toggles a clip in the multi-selection. */
    fun toggleClipSelection(vm: EditorViewModel, clipId: String) {
        val next = vm._selectedClipIds.value.toMutableSet()
        if (!next.add(clipId)) next.remove(clipId)
        vm._selectedClipIds.value = next
        vm._selectedClipId.value = next.firstOrNull()
        if (next.isNotEmpty()) vm._selectedTextId.value = null
    }

    fun clearSelection(vm: EditorViewModel) {
        // Deselect EVERYTHING: single clip, multi-select set and text overlay.
        // Clearing only the multi-select set used to leave the white selection
        // border on the timeline forever.
        vm._selectedClipId.value = null
        vm._selectedClipIds.value = emptySet()
        vm._selectedTextId.value = null
    }

    fun selectAllClips(vm: EditorViewModel) {
        val project = vm._currentProject.value ?: return
        vm._selectedClipIds.value = project.allVideoClips.map { it.id }.toSet()
        vm._selectedClipId.value = vm._selectedClipIds.value.firstOrNull()
    }

    /** True when a group operation should apply instead of the single-clip one. */
    fun hasMultiSelection(vm: EditorViewModel): Boolean = vm._selectedClipIds.value.size > 1

    /**
     * Deletes every selected clip. Main-track members ripple (the gap closes,
     * list order defines time) while overlay members lift (the remaining
     * overlays keep their absolute positions).
     */
    fun deleteSelectedClips(vm: EditorViewModel) {
        val ids = vm._selectedClipIds.value
        if (ids.isEmpty()) {
            vm.deleteSelectedClip()
            return
        }
        vm.updateProject { proj ->
            val mainSpans = proj.clips.map {
                TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs)
            }
            val byId = proj.clips.associateBy { it.id }
            val newMain = TimelineEdits.deleteAll(mainSpans, ids, ripple = true)
                .mapNotNull { span -> byId[span.id] }
            val newOverlays = proj.overlayTracks.map { track ->
                if (track.clips.none { it.id in ids }) {
                    track
                } else {
                    val spans = track.clips.map {
                        TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs)
                    }
                    val lookup = track.clips.associateBy { it.id }
                    track.copy(
                        clips = TimelineEdits.deleteAll(spans, ids, ripple = false)
                            .mapNotNull { span -> lookup[span.id] }
                    )
                }
            }
            proj.copy(clips = newMain, overlayTracks = newOverlays)
        }
        val updated = vm._currentProject.value
        val remaining = vm._selectedClipIds.value.intersect(
            (updated?.clips?.map { it.id } ?: emptyList()) +
                (updated?.overlayTracks?.flatMap { it.clips }?.map { it.id } ?: emptyList())
        ).toSet()
        vm._selectedClipIds.value = remaining
        vm._selectedClipId.value = remaining.firstOrNull()
            ?: updated?.clips?.firstOrNull()?.id
    }

    /**
     * Moves the selected overlay clips by [deltaMs] as one group, keeping their
     * relative offsets and clamping inside the project.
     */
    fun moveSelectedOverlays(vm: EditorViewModel, deltaMs: Long) {
        if (deltaMs == 0L) return
        vm.updateProject { proj ->
            val ids = vm._selectedClipIds.value
            val newOverlays = proj.overlayTracks.map { track ->
                if (track.clips.none { it.id in ids }) {
                    track
                } else {
                    val spans = track.clips.map {
                        TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs)
                    }
                    val shifted = TimelineEdits.shiftGroup(spans, ids, deltaMs)
                        .map { it.id to it.startMs }
                        .toMap()
                    track.copy(
                        clips = track.clips.map { clip ->
                            val shiftedStart = shifted[clip.id]
                            if (shiftedStart != null) {
                                clip.copy(startTimelineMs = shiftedStart)
                            } else {
                                clip
                            }
                        }
                    )
                }
            }
            proj.copy(overlayTracks = newOverlays)
        }
    }

    /** Copies selected overlay clips so they can be pasted at the playhead. */
    fun copySelectedOverlays(vm: EditorViewModel) {
        val ids = vm._selectedClipIds.value
        val project = vm._currentProject.value ?: return
        val clips = project.overlayTracks.flatMap { it.clips }.filter { it.id in ids }
        vm._overlayClipboard.value = clips.map { it.copy(id = "copy_${it.id}") }
    }

    /** Pastes the clipboard at the playhead, keeping the copied relative offsets. */
    fun pasteOverlays(vm: EditorViewModel) {
        val clipboard = vm._overlayClipboard.value
        if (clipboard.isEmpty()) return
        val baseStart = clipboard.minOf { it.startTimelineMs }
        val pasteStart = vm._currentPositionMs.value.coerceAtLeast(0L)
        vm.updateProject { proj ->
            val tracks = proj.overlayTracks.toMutableList()
            val pasted = clipboard.map { clip ->
                clip.copy(
                    id = "paste_${clip.id}",
                    startTimelineMs = (pasteStart + (clip.startTimelineMs - baseStart))
                        .coerceAtLeast(0L)
                )
            }
            if (tracks.isEmpty()) {
                tracks.add(
                    VideoTrack(
                        name = "Pasted Track",
                        isMainTrack = false,
                        clips = pasted
                    )
                )
            } else {
                tracks[0] = tracks[0].copy(clips = tracks[0].clips + pasted)
            }
            proj.copy(overlayTracks = tracks)
        }
        val newIds = vm._overlayClipboard.value.map { "paste_${it.id}" }
        vm._selectedClipIds.value = newIds.toSet()
        vm._selectedClipId.value = newIds.firstOrNull()
    }

    fun selectText(vm: EditorViewModel, textId: String?) {
        vm._selectedTextId.value = textId
        if (textId != null) {
            vm._selectedClipId.value = null
        }
    }

    fun setTimelineZoom(vm: EditorViewModel, zoom: Float) {
        vm._timelineZoom.value = zoom.coerceIn(0.5f, 3.0f)
    }
}
