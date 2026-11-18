package com.example.viewmodel.features

import com.example.data.BeatDetector
import com.example.engine.BeatGrid
import com.example.engine.ClipSplit
import com.example.model.MediaClip
import com.example.viewmodel.EditorViewModel
import java.util.UUID

/** Beat-sync: cuts the main track at detected musical beats. */
internal object BeatSyncFeature {

    /**
     * Cuts every main-track clip at the musical beats of the audio bed.
     *
     * Beats come from [BeatDetector]'s decoded PCM (or the already-detected
     * magnets), so a silent or unreadable file produces no cuts and an honest
     * status instead of a synthetic grid. The split itself goes through
     * [ClipSplit] so trims, speed, keyframes and transition ownership survive
     * the cut.
     */
    suspend fun autoCutToBeat(vm: EditorViewModel): String {
        val project = vm._currentProject.value ?: return "Open a project to sync to beats"
        if (project.isMainTrackLocked) return "Unlock the main track first"

        val beats = vm._snapBeats.value.ifEmpty {
            val sourceUri = project.audioTracks
                .firstOrNull { !it.uriString.isNullOrBlank() }
                ?.uriString
                ?: project.clips.firstOrNull { !it.uriString.isNullOrBlank() }?.uriString
                ?: return "Add an audio track to detect beats"
            val context = vm.context ?: return "Beat detection is unavailable"
            BeatDetector.detectBeats(context, sourceUri)
        }
        if (beats.isEmpty()) return "No beats detected in the audio"

        var added = 0
        vm.updateProject { current ->
            val newClips = mutableListOf<MediaClip>()
            var acc = 0L
            for (clip in current.clips) {
                val clipEnd = acc + clip.durationMs
                val localBeats = BeatGrid.cutOffsets(
                    durationMs = clip.durationMs,
                    beatOffsetsMs = beats
                        .filter { it in acc until clipEnd }
                        .map { it - acc }
                )
                if (localBeats.isEmpty()) {
                    newClips.add(clip.copy(startTimelineMs = acc))
                } else {
                    var remaining = clip
                    var consumed = 0L
                    for (beat in localBeats) {
                        val offset = beat - consumed
                        val split = ClipSplit.split(remaining, offset) {
                            "beat_${UUID.randomUUID().toString().take(8)}"
                        } ?: continue
                        newClips.add(split.first.copy(startTimelineMs = acc + consumed))
                        remaining = split.second
                        consumed = beat
                    }
                    newClips.add(remaining.copy(startTimelineMs = acc + consumed))
                }
                acc = clipEnd
            }
            added = (newClips.size - current.clips.size).coerceAtLeast(0)
            current.copy(clips = newClips)
        }

        return when {
            added > 0 -> "Cut at $added beat${if (added == 1) "" else "s"}"
            else -> "No usable beats between the clips"
        }
    }
}
