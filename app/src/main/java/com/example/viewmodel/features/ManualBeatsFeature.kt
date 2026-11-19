package com.example.viewmodel.features

import com.example.engine.BeatGrid
import com.example.viewmodel.EditorViewModel
import kotlin.math.abs

/**
 * Hand-pinned timeline beats.
 *
 * Detection cannot hear every hit (a quiet ghost note, a spoken-word bed, a
 * file the decoder cannot open), so the editor lets the user pin a beat at the
 * playhead. The pins live on [com.example.model.VideoProject.manualBeatsMs],
 * which means they are saved with the project, survive undo/redo and are merged
 * with the detected beats into the same ruler lines and snap magnets.
 */
internal object ManualBeatsFeature {

    /**
     * How close the playhead must be to a pin for "remove" to find it, and how
     * close a new pin must be to an old one to count as the same beat. A 120 BPM
     * track has a beat every 500 ms, so this tolerance never swallows the next
     * real beat.
     */
    const val MATCH_TOLERANCE_MS = 120L

    /** Pins a beat at [atMs], clamped to the project timeline. */
    fun pin(vm: EditorViewModel, atMs: Long): String {
        val project = vm._currentProject.value ?: return "Open a project to pin a beat"
        val at = atMs.coerceIn(0L, project.totalDurationMs)
        if (project.manualBeatsMs.any { abs(it - at) <= MATCH_TOLERANCE_MS }) {
            return "A beat is already pinned here"
        }
        vm.updateProject(coalesceKey = "manual-beat:pin:$at") { current ->
            current.copy(manualBeatsMs = (current.manualBeatsMs + at).distinct().sorted())
        }
        return "Beat pinned at ${label(at)}"
    }

    /**
     * Removes the pinned beat nearest to [atMs]. A pin far from the playhead is
     * left alone: silently deleting the closest beat anywhere on the timeline
     * would be surprising and un-undoable from the user's point of view.
     */
    fun unpin(vm: EditorViewModel, atMs: Long): String {
        val project = vm._currentProject.value ?: return "Open a project to edit beats"
        val nearest = project.manualBeatsMs.minByOrNull { abs(it - atMs) }
            ?: return "No pinned beats to remove"
        if (abs(nearest - atMs) > MATCH_TOLERANCE_MS) {
            return "No pinned beat at the playhead"
        }
        vm.updateProject(coalesceKey = "manual-beat:unpin:$nearest") { current ->
            current.copy(manualBeatsMs = current.manualBeatsMs.filterNot { it == nearest })
        }
        return "Beat removed from ${label(nearest)}"
    }

    /** Removes every hand-pinned beat, keeping the detected grid untouched. */
    fun clear(vm: EditorViewModel): String {
        val project = vm._currentProject.value ?: return "Open a project to edit beats"
        if (project.manualBeatsMs.isEmpty()) return "No pinned beats to clear"
        vm.updateProject(coalesceKey = "manual-beat:clear") { current ->
            current.copy(manualBeatsMs = emptyList())
        }
        return "Pinned beats cleared"
    }

    /** Absolute minutes:seconds.milliseconds, e.g. 0:03.240. */
    fun label(atMs: Long): String = BeatGrid.timecode(atMs)
}
