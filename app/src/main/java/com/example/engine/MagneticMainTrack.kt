package com.example.engine

import com.example.model.VideoProject

/**
 * The magnetic main-track rule, as a project-level transform.
 *
 * Own file on purpose: this is one of the carefully tuned CapCut-parity
 * behaviours (rule: the main video track has no gaps). Trimming, splitting,
 * duplicating or reordering a clip must ripple every following clip so they
 * stay butted together, while overlay/audio/text tracks keep their absolute
 * positions. Keeping it separate means future edits to `EditorViewModel` or
 * the timeline UI cannot quietly dilute it.
 *
 * The main track's list order is the edit order (`VideoProject.totalDurationMs`
 * sums the list), so `startTimelineMs` is *derived* here, never authored.
 */
fun VideoProject.withMagneticMainTrack(): VideoProject {
    if (clips.isEmpty()) return this
    val spans = clips.map { TimelineEdits.ClipSpan(it.id, it.startTimelineMs, it.durationMs) }
    val reflowed = TimelineEdits.magneticReflow(spans)
    if (reflowed == spans) return this
    val byId = clips.associateBy { it.id }
    return copy(
        clips = reflowed.mapNotNull { span ->
            byId[span.id]?.copy(startTimelineMs = span.startMs)
        }
    )
}
