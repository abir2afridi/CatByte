package com.example.viewmodel.features

import androidx.lifecycle.viewModelScope
import com.example.data.BeatDetector
import com.example.engine.BeatGrid
import com.example.engine.ClipSplit
import com.example.model.MediaClip
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import java.util.UUID

/** Beat-sync: cuts the main track at detected musical beats. */
internal object BeatSyncFeature {

    /**
     * Cuts every main-track clip at the musical beats of the audio bed.
     *
     * Beats come from the same detection path the timeline magnets use; the
     * split itself goes through [ClipSplit] so trims, speed, keyframes and
     * transition ownership survive the cut.
     */
    fun autoCutToBeat(vm: EditorViewModel, beatsPerMinute: Int = 120) {
        val proj = vm._currentProject.value ?: return
        val ctx = vm.context ?: return
        if (proj.isMainTrackLocked) return
        vm.viewModelScope.launch {
            val beats = if (vm._snapBeats.value.isNotEmpty()) {
                vm._snapBeats.value
            } else {
                val firstUri = proj.audioTracks.firstOrNull { !it.uriString.isNullOrBlank() }?.uriString
                    ?: proj.clips.firstOrNull { !it.uriString.isNullOrBlank() }?.uriString
                BeatDetector.detectBeats(
                    ctx, firstUri, proj.totalDurationMs, beatsPerMinute
                )
            }
            if (beats.isEmpty()) return@launch

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
                current.copy(clips = newClips)
            }
        }
    }
}
