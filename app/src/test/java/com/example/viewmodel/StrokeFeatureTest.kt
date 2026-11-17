package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Text → Draw": freehand strokes stored on the project.
 */
class StrokeFeatureTest {

    private fun viewModelWithClip(): EditorViewModel {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val clip = MediaClip(
            id = "clip_1",
            title = "First",
            durationMs = 6_000L,
            originalDurationMs = 6_000L
        )
        val project = ProjectRepository().createNewProject("Draw", initialClips = listOf(clip))
        viewModel.openProject(project)
        return viewModel
    }

    @Test
    fun `adding a stroke stores it at the playhead`() {
        val viewModel = viewModelWithClip()
        viewModel.seekTo(1_200L)

        val added = viewModel.addStroke(
            points = listOf(0.1f, 0.2f, 0.3f, 0.4f),
            colorArgb = 0xFFFF3B30,
            widthFraction = 0.012f
        )

        assertTrue(added)
        val stroke = viewModel.currentProject.value!!.strokes.single()
        assertEquals(1_200L, stroke.startTimelineMs)
        assertEquals(4, stroke.points.size)
        assertEquals(0xFFFF3B30, stroke.colorArgb)
    }

    @Test
    fun `a tap without movement is rejected`() {
        val viewModel = viewModelWithClip()

        val added = viewModel.addStroke(listOf(0.5f, 0.5f), 0xFFFFFFFF, 0.01f)

        assertFalse(added)
        assertTrue(viewModel.currentProject.value!!.strokes.isEmpty())
    }

    @Test
    fun `points are clamped into the canvas`() {
        val viewModel = viewModelWithClip()

        viewModel.addStroke(
            points = listOf(-0.5f, 1.9f, 0.2f, 0.3f),
            colorArgb = 0xFF26E5D7,
            widthFraction = 0.5f
        )

        val stroke = viewModel.currentProject.value!!.strokes.single()
        assertEquals(0f, stroke.points[0], 0.0001f)
        assertEquals(1f, stroke.points[1], 0.0001f)
        // Width is clamped to a sane fraction of the canvas.
        assertEquals(0.06f, stroke.widthFraction, 0.0001f)
    }

    @Test
    fun `clear removes every stroke`() {
        val viewModel = viewModelWithClip()
        viewModel.addStroke(listOf(0.1f, 0.1f, 0.2f, 0.2f), 0xFFFFFFFF, 0.01f)
        viewModel.addStroke(listOf(0.3f, 0.3f, 0.4f, 0.4f), 0xFFFFFFFF, 0.01f)

        viewModel.clearStrokes()

        assertTrue(viewModel.currentProject.value!!.strokes.isEmpty())
    }

    @Test
    fun `a stroke is undoable`() {
        val viewModel = viewModelWithClip()
        viewModel.addStroke(listOf(0.1f, 0.1f, 0.2f, 0.2f), 0xFFFFFFFF, 0.01f)

        viewModel.undo()

        assertTrue(viewModel.currentProject.value!!.strokes.isEmpty())
    }
}
