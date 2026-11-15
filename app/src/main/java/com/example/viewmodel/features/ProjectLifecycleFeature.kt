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
        vm.cancelScrubPreview()
        vm.pausePlayback()
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
    }

    /** Clears ALL projects from the repository (database + memory). */
    fun clearAllProjects(vm: EditorViewModel) {
        vm._currentProject.value = null
        vm.repository.deleteAllProjects()
        vm.playerManager?.pause()
    }

    fun setActiveSheet(vm: EditorViewModel, sheet: ActiveToolSheet) {
        vm._activeSheet.value = sheet
    }
}
