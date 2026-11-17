package com.example.data

import androidx.media3.common.C
import androidx.media3.transformer.EditedMediaItemSequence
import com.example.model.MediaClip
import com.example.model.VideoProject

/** The sequence layout and compositor overlay-index map for one composition. */
internal data class CompositionSequences(
    val sequences: List<EditedMediaItemSequence>,
    val overlayInputs: Map<Int, MediaClip>
)

/**
 * Builds the primary video sequence, one sequence per overlay clip, and the
 * independent audio sequences. Sequence index is exactly the video compositor
 * input index, which is what `overlayInputs` records.
 */
internal fun ProjectCompositionFactory.buildSequences(
    project: VideoProject,
    renderPlan: ProjectCompositionFactory.RenderPlan,
    fps: Int,
    warnings: MutableSet<String>,
    removeVideo: Boolean = false
): CompositionSequences {
    val sequences = mutableListOf<EditedMediaItemSequence>()
    val overlayInputs = linkedMapOf<Int, MediaClip>()

    // 1. Primary video sequence. Placeholder clips become real timeline gaps
    // so the main track always occupies the same absolute time as the editor.
    val mainBuilder = EditedMediaItemSequence.Builder(
        setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO)
    )
    var cursorUs = 0L
    renderPlan.main.forEach { planned ->
        val gapUs = planned.layer.timelineInUs - cursorUs
        if (gapUs > 0L) {
            mainBuilder.addGap(gapUs)
        }
        if (planned.clip.uriString.isNullOrBlank()) {
            if (planned.layer.durationUs > 0L) {
                mainBuilder.addGap(planned.layer.durationUs)
            }
        } else {
            mainBuilder.addItem(buildVideoItem(project, planned, fps, warnings, removeVideo))
        }
        cursorUs = planned.layer.timelineOutUs
    }
    val totalDurationUs = renderPlan.totalDurationUs.coerceAtLeast(1_000_000L)
    if (cursorUs < totalDurationUs) {
        // Pad the primary sequence so overlays/audio near the end are not cut
        // off by a shorter primary track.
        mainBuilder.addGap(totalDurationUs - cursorUs)
    }
    sequences += mainBuilder.build()

    // 2. Every PIP / overlay clip gets its own sequence. Sequence index is
    // exactly the video compositor input index; the graph supplies its start.
    renderPlan.overlays.forEach { planned ->
        if (planned.clip.uriString.isNullOrBlank()) {
            return@forEach
        }
        val builder = EditedMediaItemSequence.Builder(
            setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO)
        )
        if (planned.layer.timelineInUs > 0L) {
            builder.addGap(planned.layer.timelineInUs)
        }
        builder.addItem(buildVideoItem(project, planned, fps, warnings, removeVideo))
        val inputIndex = sequences.size
        sequences += builder.build()
        overlayInputs[inputIndex] = planned.clip
    }

    // 3. Independent audio tracks are mixed by CompositionPlayer during
    // preview and by Transformer during export.
    // Voice-over spans drive auto-ducking for every music clip below.
    val voiceRanges = project.audioTracks
        .filter { it.isVoiceover && !it.isMuted && it.volume > 0.001f && it.durationMs > 0L }
        .map { it.startTimelineMs until (it.startTimelineMs + it.durationMs) }
    renderPlan.audio.forEach { (audio, spec) ->
        sequences += buildAudioSequence(audio, spec, voiceRanges)
    }

    return CompositionSequences(sequences = sequences, overlayInputs = overlayInputs)
}
