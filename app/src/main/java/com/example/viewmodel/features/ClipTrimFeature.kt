package com.example.viewmodel.features

import com.example.engine.ClipTrimHandle
import com.example.viewmodel.ClipTrimSession
import com.example.viewmodel.EditorViewModel

/**
 * Inline trim handles: begin, live update and end of a handle drag.
 *
 * The drag session is per-editor state, so this is a class (one instance per
 * editor) instead of a shared object.
 */
internal class ClipTrimFeature(private val vm: EditorViewModel) {

    /** One inline handle drag, snapshotted when the finger goes down. */
    private var trimSession: ClipTrimSession? = null

    /**
     * Trims the selected clip from an inline timeline handle.
     *
     * [ClipTrimHandle] owns the source maths; this only applies the result, so the
     * sheet trim and the handles can never disagree. Locked tracks refuse it.
     */
    fun trimSelectedClipEdge(edge: ClipTrimHandle.Edge, outputDeltaMs: Long, fps: Int) {
        val clipId = vm._selectedClipId.value ?: return
        beginTrimClip(clipId, edge)
        updateTrimClip(outputDeltaMs, fps)
        endTrimClip()
    }

    /** Snapshots the clip before a handle drag so every live update is absolute. */
    fun beginTrimClip(clipId: String, edge: ClipTrimHandle.Edge) {
        val proj = vm._currentProject.value
        if (proj == null || proj.isTrackLockedForClip(clipId)) {
            trimSession = null
            return
        }
        val clip = (proj.clips + proj.overlayTracks.flatMap { it.clips })
            .firstOrNull { it.id == clipId }
        trimSession = clip?.let { ClipTrimSession(clipId, edge, it, proj) }
        // No rebuilds while the handle moves (see PreviewRefreshScheduler);
        // endTrimClip fires exactly one rebuild at finger lift.
        if (trimSession != null) {
            vm.previewScheduler.beginSuppression()
        }
    }

    /**
     * Live trim while the handle moves. All the rules (in/out only, magnetic
     * reflow, overlay end pinning, idempotent absolute application) live in
     * [ClipTrimSession]; this only wires state/history/preview/save.
     */
    fun updateTrimClip(cumulativeDeltaMs: Long, fps: Int) {
        val session = trimSession ?: return
        val curr = vm._currentProject.value ?: return
        val updated = session.applyTo(curr, cumulativeDeltaMs, fps) ?: return
        if (!session.historyPushed) {
            session.historyPushed = true
            // ONE undo entry per drag: the pre-drag project.
            vm.pushHistory(session.baseProject, "trim:${session.clipId}:${session.edge}")
        }
        vm._currentProject.value = updated
        // Trimming can make the timeline shorter than the playhead.
        val clampedPosition = vm._currentPositionMs.value.coerceIn(0L, updated.totalDurationMs)
        if (clampedPosition != vm._currentPositionMs.value) {
            vm._currentPositionMs.value = clampedPosition
            vm.playerManager?.seekTo(clampedPosition)
        }
        vm.schedulePreviewRefresh()
    }

    /** Ends the handle drag; autosaves once and refreshes the preview once. */
    fun endTrimClip() {
        val hadSession = trimSession != null
        trimSession = null
        if (hadSession) {
            vm._currentProject.value?.let { vm.repository.saveProject(it) }
            // One rebuild at finger lift: the preview shows the final trim
            // immediately, without any mid-drag composition rebuilds.
            vm.previewScheduler.endSuppression()
        }
    }
}
