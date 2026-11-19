package com.example.viewmodel.features

import com.example.engine.TimelineEdits
import com.example.model.MediaClip
import com.example.model.VideoTrack
import com.example.viewmodel.EditorViewModel
import java.util.UUID

/** Clip/text selection, multi-selection, overlay clipboard and timeline zoom. */
internal object SelectionFeature {

    /**
     * One copied clip plus the timeline information needed to paste it back.
     * Main clips keep their order and are inserted at the playhead; overlays
     * keep their absolute start offsets relative to the first copied overlay.
     */
    internal data class ClipboardEntry(
        val clip: MediaClip,
        val isMain: Boolean,
        val overlayTrackId: String?,
        val timelineStartMs: Long,
        val mainIndex: Int
    )

    private fun selectedIds(vm: EditorViewModel): Set<String> {
        val ids = vm._selectedClipIds.value
        if (ids.isNotEmpty()) return ids
        val single = vm._selectedClipId.value ?: return emptySet()
        return setOf(single)
    }

    fun selectClip(vm: EditorViewModel, clipId: String?) {
        // The "original" comparison belongs to the clip that was graded, so a
        // new selection ends it.
        if (vm._compareOriginal.value) vm.setCompareOriginal(false)
        vm._selectedClipId.value = clipId
        vm._selectedClipIds.value = if (clipId == null) emptySet() else setOf(clipId)
        vm._selectedAudioId.value = null
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
        vm._selectedAudioId.value = null
        if (next.isNotEmpty()) vm._selectedTextId.value = null
    }

    fun clearSelection(vm: EditorViewModel) {
        // Deselect EVERYTHING: single clip, multi-select set and text overlay.
        // Clearing only the multi-select set used to leave the white selection
        // border on the timeline forever.
        if (vm._compareOriginal.value) vm.setCompareOriginal(false)
        vm._selectedClipId.value = null
        vm._selectedClipIds.value = emptySet()
        vm._selectedTextId.value = null
        vm._selectedAudioId.value = null
    }

    fun selectAllClips(vm: EditorViewModel) {
        val project = vm._currentProject.value ?: return
        vm._selectedClipIds.value = project.allVideoClips.map { it.id }.toSet()
        vm._selectedClipId.value = vm._selectedClipIds.value.firstOrNull()
        vm._selectedAudioId.value = null
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

    /** Copies main-track and overlay clips; unlike the legacy helper it never drops main clips. */
    fun copySelectedClips(vm: EditorViewModel): List<ClipboardEntry> {
        val project = vm._currentProject.value ?: return emptyList()
        val ids = selectedIds(vm)
        if (ids.isEmpty()) return emptyList()

        val entries = mutableListOf<ClipboardEntry>()
        var cursorMs = 0L
        project.clips.forEachIndexed { index, clip ->
            if (clip.id in ids) {
                entries += ClipboardEntry(
                    clip = clip,
                    isMain = true,
                    overlayTrackId = null,
                    timelineStartMs = cursorMs,
                    mainIndex = index
                )
            }
            cursorMs += clip.durationMs.coerceAtLeast(0L)
        }
        project.overlayTracks.forEach { track ->
            track.clips.forEach { clip ->
                if (clip.id in ids) {
                    entries += ClipboardEntry(
                        clip = clip,
                        isMain = false,
                        overlayTrackId = track.id,
                        timelineStartMs = clip.startTimelineMs.coerceAtLeast(0L),
                        mainIndex = -1
                    )
                }
            }
        }

        vm.updateClipboard(entries)
        // Keep the legacy flow in sync so existing overlay-only callers/tests
        // continue to work with the new unified clipboard.
        vm._overlayClipboard.value = entries
            .filter { !it.isMain }
            .map { it.clip.copy(id = "copy_${it.clip.id}") }
        return entries
    }

    /** Copies the selection and removes it in one undoable edit. */
    fun cutSelectedClips(vm: EditorViewModel) {
        if (copySelectedClips(vm).isEmpty()) return
        if (selectedIds(vm).isEmpty()) return
        vm.deleteSelectedClips()
    }

    /**
     * Pastes the clipboard at the playhead in one undoable edit. Main clips are
     * inserted in their copied order at the playhead; overlays keep their
     * relative offsets and their original tracks where those still exist.
     */
    fun pasteClips(vm: EditorViewModel) {
        val entries = vm._clipboard.value
        if (entries.isEmpty()) return
        val pasteStartMs = vm._currentPositionMs.value.coerceAtLeast(0L)
        val pastedIds = mutableListOf<String>()

        vm.updateProject { proj ->
            var updated = proj

            val mainEntries = entries.filter { it.isMain }.sortedBy { it.mainIndex }
            if (mainEntries.isNotEmpty()) {
                var cursorMs = 0L
                var insertIndex = proj.clips.size
                for (index in proj.clips.indices) {
                    if (cursorMs >= pasteStartMs) {
                        insertIndex = index
                        break
                    }
                    cursorMs += proj.clips[index].durationMs.coerceAtLeast(0L)
                }
                val pasted = mainEntries.map { entry ->
                    entry.clip.copy(id = newPasteId()).also { pastedIds += it.id }
                }
                val clips = proj.clips.toMutableList()
                clips.addAll(insertIndex, pasted)
                updated = updated.copy(clips = clips)
            }

            val overlayEntries = entries.filter { !it.isMain }
            if (overlayEntries.isNotEmpty()) {
                val baseStart = overlayEntries.minOf { it.timelineStartMs }
                val clipsByTrack = linkedMapOf<String, MutableList<MediaClip>>()
                overlayEntries.forEach { entry ->
                    val targetTrackId = if (proj.overlayTracks.any { it.id == entry.overlayTrackId }) {
                        entry.overlayTrackId!!
                    } else {
                        proj.overlayTracks.firstOrNull()?.id ?: "pasted_track"
                    }
                    val clip = entry.clip.copy(
                        id = newPasteId(),
                        startTimelineMs = pasteStartMs + (entry.timelineStartMs - baseStart).coerceAtLeast(0L)
                    )
                    pastedIds += clip.id
                    clipsByTrack.getOrPut(targetTrackId) { mutableListOf() } += clip
                }
                val tracks = proj.overlayTracks.map { track ->
                    val additions = clipsByTrack[track.id]
                    if (additions.isNullOrEmpty()) track else track.copy(clips = track.clips + additions)
                }.toMutableList()
                clipsByTrack["pasted_track"]?.let { additions ->
                    if (proj.overlayTracks.none { it.id == "pasted_track" }) {
                        tracks.add(
                            VideoTrack(
                                id = "pasted_track",
                                name = "Pasted Track",
                                isMainTrack = false,
                                clips = additions
                            )
                        )
                    }
                }
                updated = updated.copy(overlayTracks = tracks)
            }
            updated
        }

        vm._selectedClipIds.value = pastedIds.toSet()
        vm._selectedClipId.value = pastedIds.firstOrNull()
    }

    private fun newPasteId(): String = "paste_${UUID.randomUUID()}"

    fun selectText(vm: EditorViewModel, textId: String?) {
        vm._selectedTextId.value = textId
        vm._selectedAudioId.value = null
        if (textId != null) {
            vm._selectedClipId.value = null
        }
    }

    /** Selects an independent audio clip and clears video/text selection. */
    fun selectAudioClip(vm: EditorViewModel, audioId: String?) {
        vm._selectedAudioId.value = audioId
        if (audioId != null) {
            vm._selectedClipId.value = null
            vm._selectedClipIds.value = emptySet()
            vm._selectedTextId.value = null
        }
    }

    fun setTimelineZoom(vm: EditorViewModel, zoom: Float) {
        vm._timelineZoom.value = zoom.coerceIn(0.5f, 3.0f)
    }
}
