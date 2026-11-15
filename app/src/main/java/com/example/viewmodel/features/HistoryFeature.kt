package com.example.viewmodel.features

import com.example.data.db.ProjectJson
import com.example.engine.withMagneticMainTrack
import com.example.model.VideoProject
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.EditorViewModel.EditorHistoryEntry

/**
 * Undo/redo history and the central project mutation helper.
 *
 * The history is a bounded stack of immutable snapshots with structural sharing
 * (data-class `copy` shares unchanged lists), so snapshots stay cheap even at a
 * deep history. A durable journal mirrors the in-memory stack so a crash does
 * not destroy every undo step of the session.
 */
internal object HistoryFeature {

    private const val HISTORY_MAX = 200

    /** Drag/slider streams inside this window collapse into ONE undo step. */
    private const val COALESCE_WINDOW_MS = 500L

    fun pushHistory(vm: EditorViewModel, project: VideoProject, coalesceKey: String? = null) {
        val now = System.currentTimeMillis()
        val last = vm.undoStack.lastOrNull()
        if (coalesceKey != null && last != null &&
            last.key == coalesceKey && now - last.atMs < COALESCE_WINDOW_MS
        ) {
            // Same gesture is still streaming: keep the pre-gesture snapshot
            // on top so ONE undo reverts the whole drag (CapCut behaviour).
            // Slide the timestamp so a long drag stays a single undo step.
            vm.undoStack[vm.undoStack.lastIndex] = last.copy(atMs = now)
        } else {
            vm.undoStack.add(EditorHistoryEntry(project, coalesceKey, now))
            if (vm.undoStack.size > HISTORY_MAX) {
                vm.undoStack.removeAt(0)
            }
            vm.redoStack.clear()
        }
        vm._canUndo.value = vm.undoStack.isNotEmpty()
        vm._canRedo.value = vm.redoStack.isNotEmpty()
        vm.undoJournal?.record(ProjectJson.toJson(project), coalesceKey)
    }

    private fun applyHistoryState(vm: EditorViewModel, project: VideoProject) {
        val normalized = project.withMagneticMainTrack()
        vm._currentProject.value = normalized
        vm.repository.saveProject(normalized)
        vm._canUndo.value = vm.undoStack.isNotEmpty() || vm.undoJournal?.canUndo == true
        vm._canRedo.value = vm.redoStack.isNotEmpty() || vm.undoJournal?.canRedo == true
        vm.schedulePreviewRefresh()
    }

    fun undo(vm: EditorViewModel) {
        val curr = vm._currentProject.value ?: return
        // Advance the durable journal alongside the in-memory stack so both stay
        // aligned; when the stack is empty after a restart, the journal answers.
        val journalState = vm.undoJournal?.undo(ProjectJson.toJson(curr))?.let { ProjectJson.fromJson(it) }
        if (vm.undoStack.isNotEmpty()) {
            vm.redoStack.add(curr)
            applyHistoryState(vm, vm.undoStack.removeAt(vm.undoStack.lastIndex).project)
        } else if (journalState != null && journalState.id == curr.id) {
            applyHistoryState(vm, journalState)
        }
    }

    fun redo(vm: EditorViewModel) {
        val curr = vm._currentProject.value ?: return
        val journalState = vm.undoJournal?.redo(ProjectJson.toJson(curr))?.let { ProjectJson.fromJson(it) }
        if (vm.redoStack.isNotEmpty()) {
            vm.undoStack.add(EditorHistoryEntry(curr))
            applyHistoryState(vm, vm.redoStack.removeAt(vm.redoStack.lastIndex))
        } else if (journalState != null && journalState.id == curr.id) {
            applyHistoryState(vm, journalState)
        }
    }

    /**
     * Central mutation helper: snapshots the pre-edit project for undo, applies
     * [transform] and autosaves. [coalesceKey] groups a drag/slider stream so
     * one gesture becomes exactly one undo step (plan §12 coalescing rule).
     */
    fun updateProject(
        vm: EditorViewModel,
        coalesceKey: String? = null,
        transform: (VideoProject) -> VideoProject
    ) {
        val curr = vm._currentProject.value ?: return
        pushHistory(vm, curr, coalesceKey)
        val updated = transform(curr).withMagneticMainTrack()
        vm._currentProject.value = updated
        vm.repository.saveProject(updated)
        // Trimming can make the timeline shorter than the playhead.
        val clampedPosition = vm._currentPositionMs.value.coerceIn(0L, updated.totalDurationMs)
        if (clampedPosition != vm._currentPositionMs.value) {
            vm._currentPositionMs.value = clampedPosition
            vm.playerManager?.seekTo(clampedPosition)
        }
        vm.schedulePreviewRefresh()
    }
}
