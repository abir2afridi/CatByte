package com.example

import com.example.data.ProjectRepository
import com.example.data.SampleDataProvider
import com.example.model.AspectRatioType
import com.example.model.VideoFilter
import com.example.viewmodel.EditorViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun projectRepository_initialProjects_loaded() {
        val repo = ProjectRepository()
        val projects = repo.projects.value
        assertTrue(projects.isNotEmpty())
        val first = projects.first()
        assertTrue(first.clips.isNotEmpty())
    }

    @Test
    fun editorViewModel_projectCreation_works() {
        val repo = ProjectRepository()
        val viewModel = EditorViewModel(repo)

        assertNotNull(viewModel.currentProject.value)
        viewModel.setAspectRatio(AspectRatioType.RATIO_16_9)
        assertEquals(AspectRatioType.RATIO_16_9, viewModel.currentProject.value?.aspectRatio)
    }

    @Test
    fun editorViewModel_clipFilter_updates() {
        val repo = ProjectRepository()
        val viewModel = EditorViewModel(repo)
        val clipId = viewModel.currentProject.value?.clips?.firstOrNull()?.id
        assertNotNull(clipId)

        viewModel.selectClip(clipId)
        viewModel.setClipFilter(VideoFilter.CYBERPUNK)

        val updatedClip = viewModel.currentProject.value?.clips?.find { it.id == clipId }
        assertEquals(VideoFilter.CYBERPUNK, updatedClip?.filter)
    }

    @Test
    fun projectRepository_toggleFavorite_updatesState() {
        val repo = ProjectRepository()
        val project = repo.projects.value.first()
        val initialFav = project.isFavorite
        repo.toggleFavorite(project.id)
        val updatedProject = repo.projects.value.find { it.id == project.id }
        assertEquals(!initialFav, updatedProject?.isFavorite)
    }

    @Test
    fun editorViewModel_undoRedo_tracksCorrectly() {
        val repo = ProjectRepository()
        val viewModel = EditorViewModel(repo)

        assertFalse(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        // Make an action
        viewModel.setAspectRatio(AspectRatioType.RATIO_1_1)
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        // Undo
        viewModel.undo()
        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)

        // Redo
        viewModel.redo()
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
    }

    @Test
    fun editorViewModel_magneticSnap_togglesCorrectly() {
        val repo = ProjectRepository()
        val viewModel = EditorViewModel(repo)

        assertTrue(viewModel.magneticSnap.value)
        viewModel.toggleMagneticSnap()
        assertFalse(viewModel.magneticSnap.value)
        viewModel.toggleMagneticSnap()
        assertTrue(viewModel.magneticSnap.value)
    }

    @Test
    fun editorViewModel_createCustomProject_initializesProperly() {
        val repo = ProjectRepository()
        val viewModel = EditorViewModel(repo)

        viewModel.createNewProjectWithParams("Blueprint Sketch", AspectRatioType.RATIO_16_9, "Drafting Grid")
        val current = viewModel.currentProject.value
        assertNotNull(current)
        assertEquals("Blueprint Sketch", current?.title)
        assertEquals(AspectRatioType.RATIO_16_9, current?.aspectRatio)
        assertEquals("Drafting Grid", current?.paperStyle)
    }
}
