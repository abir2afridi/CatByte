package com.example.viewmodel.features

import com.example.data.AppLog
import com.example.engine.withMagneticMainTrack
import com.example.model.AspectRatioType
import com.example.model.VideoProject
import com.example.viewmodel.ActiveToolSheet
import com.example.viewmodel.EditorViewModel

/** Project open/create/duplicate/delete/restore and the active tool sheet. */
internal object ProjectLifecycleFeature {

    fun openProject(vm: EditorViewModel, project: VideoProject) {
        AppLog.info("EditorViewModel", "openProject() called: projectId=${project.id}, title=${project.title}, clips=${project.clips.size}, totalDuration=${project.totalDurationMs}ms")
        // The previous project's scrub overlay and settle loop must not leak into
        // the new project's preview.
        vm.scrubPreview.cancel()
        vm.pausePlayback()
        // A new project has its own grade: the comparison of the old project
        // must not survive the switch.
        vm.clearCompareOriginal()
        val normalized = project.withMagneticMainTrack()
        // Start the preview at 0 explicitly: leaving the default would keep the
        // previous project's playhead inside PlayerManager, and the UI would
        // show 0 while playback resumed somewhere else.
        vm.playerManager?.loadProject(normalized, startPositionMs = 0L)
        vm._currentProject.value = normalized
        vm._currentPositionMs.value = 0L
        // Opening a project never pre-selects a clip (CapCut behaviour).
        vm.clearSelection()
        vm._activeSheet.value = ActiveToolSheet.NONE
        vm.undoStack.clear()
        vm.redoStack.clear()
        vm._canUndo.value = false
        vm._canRedo.value = false
        // A project can outlive the media it points at; check right away so the
        // editor can offer relinking instead of showing a silent black frame.
        RelinkFeature.refresh(vm)
        AppLog.debug("EditorViewModel", "openProject() completed, currentProject set")
    }

    fun toggleProjectFavorite(vm: EditorViewModel, id: String) {
        vm.repository.toggleFavorite(id)
        if (vm._currentProject.value?.id == id) {
            vm._currentProject.value = vm._currentProject.value?.copy(
                isFavorite = !(vm._currentProject.value?.isFavorite ?: false)
            )
        }
    }

    fun createNewProjectWithParams(
        vm: EditorViewModel,
        title: String,
        ratio: AspectRatioType = AspectRatioType.RATIO_9_16,
        paperStyle: String = "Standard"
    ) {
        vm.pausePlayback()
        val proj = vm.repository.createNewProject(
            title = title.ifBlank { "New Project #${System.currentTimeMillis() % 1000}" },
            aspectRatio = ratio,
            paperStyle = paperStyle,
            initialClips = emptyList(),
            resolution = vm._defaultResolution.value,
            fps = vm._defaultFps.value
        )
        openProject(vm, proj)
    }

    fun createNewProject(vm: EditorViewModel) {
        createNewProjectWithParams(vm, "New Project #${System.currentTimeMillis() % 1000}")
    }

    /**
     * Renames a project from the home list. The open project is updated in place
     * so the editor title changes immediately, without reloading the player.
     */
    fun renameProject(vm: EditorViewModel, id: String, title: String) {
        val renamed = vm.repository.renameProject(id, title) ?: return
        if (vm._currentProject.value?.id == id) {
            vm._currentProject.value = renamed
        }
    }

    fun duplicateProject(vm: EditorViewModel, id: String) {
        vm.repository.duplicateProject(id)
        vm._currentProject.value?.let { vm.playerManager?.loadProject(it) }
    }

    fun deleteProject(vm: EditorViewModel, id: String) {
        if (vm._currentProject.value?.id == id) {
            vm._currentProject.value = null
        }
        vm.repository.deleteProject(id)
        vm._currentProject.value?.let { vm.playerManager?.loadProject(it) } ?: vm.playerManager?.pause()
    }

    /** Restores a draft removed from the home screen (undo delete). */
    fun restoreProject(vm: EditorViewModel, project: VideoProject) {
        vm.repository.saveProject(project)
        vm._currentProject.value = project
        vm.playerManager?.loadProject(project)
        RelinkFeature.refresh(vm)
    }

    /** Clears ALL projects from the repository (database + memory). */
    fun clearAllProjects(vm: EditorViewModel) {
        vm._currentProject.value = null
        vm.repository.deleteAllProjects()
        vm.playerManager?.pause()
    }

    fun setActiveSheet(vm: EditorViewModel, sheet: ActiveToolSheet) {
        // The comparison belongs to the Adjust panel: leaving it ends the
        // comparison, so the preview never stays stuck on the original.
        if (sheet != ActiveToolSheet.ADJUSTMENTS && vm._compareOriginal.value) {
            vm.setCompareOriginal(false)
        }
        vm._activeSheet.value = sheet
    }
}
