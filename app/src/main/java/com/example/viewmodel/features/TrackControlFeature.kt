package com.example.viewmodel.features

import com.example.engine.SnapEngine
import com.example.model.TimelineTrackIds
import com.example.viewmodel.EditorViewModel

/** Track and clip mute/lock/solo controls, track reordering and clip moves. */
internal object TrackControlFeature {

    /** Track-level mute. Passes [TimelineTrackIds.MAIN] for the main video track. */
    fun setTrackMuted(vm: EditorViewModel, trackId: String, muted: Boolean) {
        vm.updateProject { proj ->
            if (trackId == TimelineTrackIds.MAIN) {
                return@updateProject proj.copy(isMainTrackMuted = muted)
            }
            proj.copy(
                overlayTracks = proj.overlayTracks.map { track ->
                    if (track.id == trackId) track.copy(isMuted = muted) else track
                }
            )
        }
    }

    fun setTrackLocked(vm: EditorViewModel, trackId: String, locked: Boolean) {
        vm.updateProject { proj ->
            if (trackId == TimelineTrackIds.MAIN) {
                return@updateProject proj.copy(isMainTrackLocked = locked)
            }
            proj.copy(
                overlayTracks = proj.overlayTracks.map { track ->
                    if (track.id == trackId) track.copy(isLocked = locked) else track
                }
            )
        }
    }

    /** Solo silences every other video track's audio, like an audio desk. */
    fun setTrackSolo(vm: EditorViewModel, trackId: String, solo: Boolean) {
        vm.updateProject { proj ->
            if (trackId == TimelineTrackIds.MAIN) {
                return@updateProject proj.copy(isMainTrackSolo = solo)
            }
            val cleared = if (solo) proj.copy(
                isMainTrackSolo = false,
                overlayTracks = proj.overlayTracks.map { it.copy(isSolo = false) }
            ) else {
                proj
            }
            if (solo) {
                cleared.copy(
                    overlayTracks = cleared.overlayTracks.map { track ->
                        if (track.id == trackId) track.copy(isSolo = true) else track
                    }
                )
            } else {
                cleared
            }
        }
    }

    /** Mutes or unmutes a single clip (audio gain only; the video stays visible). */
    fun setClipMuted(vm: EditorViewModel, clipId: String, muted: Boolean) {
        vm.updateProject { proj ->
            proj.copy(
                clips = proj.clips.map { if (it.id == clipId) it.copy(isMuted = muted) else it },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(
                        clips = track.clips.map {
                            if (it.id == clipId) it.copy(isMuted = muted) else it
                        }
                    )
                }
            )
        }
    }

    /** Mutes or unmutes one audio clip (the per-audio-row header toggle). */
    fun setAudioClipMuted(vm: EditorViewModel, audioId: String, muted: Boolean) {
        vm.updateProject { proj ->
            proj.copy(
                audioTracks = proj.audioTracks.map { audio ->
                    if (audio.id == audioId) audio.copy(isMuted = muted) else audio
                }
            )
        }
    }

    /** Mutes or unmutes every audio clip at once (the "Audio" row header). */
    fun setAllAudioMuted(vm: EditorViewModel, muted: Boolean) {
        val project = vm._currentProject.value ?: return
        if (project.audioTracks.isEmpty()) return
        vm.updateProject { proj ->
            proj.copy(audioTracks = proj.audioTracks.map { it.copy(isMuted = muted) })
        }
    }

    /** Reorders overlay tracks (top row = first layer rendered). */
    fun moveOverlayTrack(vm: EditorViewModel, fromIndex: Int, toIndex: Int) {
        vm.updateProject { proj ->
            if (fromIndex !in proj.overlayTracks.indices) return@updateProject proj
            if (toIndex !in proj.overlayTracks.indices) return@updateProject proj
            if (fromIndex == toIndex) return@updateProject proj
            val tracks = proj.overlayTracks.toMutableList()
            val track = tracks.removeAt(fromIndex)
            tracks.add(toIndex, track)
            proj.copy(overlayTracks = tracks)
        }
    }

    /**
     * Moves an audio clip to a new start time. The drop snaps to neighbouring
     * audio clips and clamps inside the project.
     */
    fun moveAudioClip(vm: EditorViewModel, audioId: String, desiredStartMs: Long) {
        vm.updateProject { proj ->
            val clip = proj.audioTracks.firstOrNull { it.id == audioId } ?: return@updateProject proj
            val others = proj.audioTracks.filter { it.id != audioId }
            val occupied = SnapEngine.occupiedRanges(
                others.map { it.startTimelineMs to it.durationMs }
            )
            val snapped = SnapEngine.magneticStart(desiredStartMs, clip.durationMs, occupied)
            val maxStart = (proj.totalDurationMs - clip.durationMs).coerceAtLeast(0L)
            val start = snapped.coerceIn(0L, maxStart)
            proj.copy(
                audioTracks = proj.audioTracks.map { existing ->
                    if (existing.id == audioId) existing.copy(startTimelineMs = start) else existing
                }
            )
        }
    }

    /**
     * Moves the selected main-track clips as one contiguous block by [delta]
     * positions. A group keeps its internal order; neighbours close the gap.
     */
    fun moveSelectedMainClips(vm: EditorViewModel, delta: Int) {
        if (delta == 0) return
        vm.updateProject { proj ->
            if (proj.isMainTrackLocked) return@updateProject proj
            val ids = vm._selectedClipIds.value
            if (ids.isEmpty()) return@updateProject proj
            val selectedIndices = proj.clips
                .withIndex()
                .filter { it.value.id in ids }
                .map { it.index }
            if (selectedIndices.isEmpty()) return@updateProject proj

            // Only a contiguous block can move as one group.
            val isContiguous = selectedIndices.zipWithNext().all { (a, b) -> b == a + 1 }
            if (!isContiguous) return@updateProject proj

            val first = selectedIndices.first()
            val last = selectedIndices.last()
            val block = proj.clips.subList(first, last + 1)
            val targetFirst = (first + delta).coerceIn(0, proj.clips.size - block.size)
            if (targetFirst == first) return@updateProject proj

            val clips = proj.clips.toMutableList()
            clips.subList(first, last + 1).clear()
            clips.addAll(targetFirst, block)
            proj.copy(clips = clips)
        }
    }
}
