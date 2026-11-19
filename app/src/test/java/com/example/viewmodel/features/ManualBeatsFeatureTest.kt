package com.example.viewmodel.features

import com.example.data.ProjectRepository
import com.example.model.MediaClip
import com.example.model.VideoProject
import com.example.viewmodel.EditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hand-pinned beats must behave like real project data: clamped to the
 * timeline, one pin per beat, undoable and observable through the project.
 */
class ManualBeatsFeatureTest {

    private fun viewModel(manual: List<Long> = emptyList()): EditorViewModel {
        val project = VideoProject(
            id = "beat_project",
            title = "Beats",
            clips = listOf(
                MediaClip(
                    id = "video",
                    title = "Video",
                    durationMs = 5_000L,
                    originalDurationMs = 5_000L
                )
            ),
            manualBeatsMs = manual
        )
        return EditorViewModel(repository = ProjectRepository()).apply { openProject(project) }
    }

    @Test
    fun `pin clamps to the timeline and keeps the list ordered`() {
        val viewModel = viewModel()

        assertEquals("Beat pinned at 0:00.000", viewModel.pinBeatAt(-500L))
        assertEquals("Beat pinned at 0:01.200", viewModel.pinBeatAt(1_200L))
        assertEquals("Beat pinned at 0:00.700", viewModel.pinBeatAt(700L))

        assertEquals(
            listOf(0L, 700L, 1_200L),
            viewModel.currentProject.value!!.manualBeatsMs
        )
    }

    @Test
    fun `pin past the end of the project lands on the last millisecond`() {
        val viewModel = viewModel()
        viewModel.pinBeatAt(9_000L)
        assertEquals(listOf(5_000L), viewModel.currentProject.value!!.manualBeatsMs)
    }

    @Test
    fun `pin refuses a duplicate inside the beat tolerance`() {
        val viewModel = viewModel(manual = listOf(1_000L))

        assertEquals("A beat is already pinned here", viewModel.pinBeatAt(1_050L))
        assertEquals(listOf(1_000L), viewModel.currentProject.value!!.manualBeatsMs)

        // A refused pin must not create an undo step either.
        viewModel.undo()
        assertEquals(listOf(1_000L), viewModel.currentProject.value!!.manualBeatsMs)
    }

    @Test
    fun `pin supports undo and redo`() {
        val viewModel = viewModel()
        viewModel.pinBeatAt(1_200L)
        assertEquals(listOf(1_200L), viewModel.currentProject.value!!.manualBeatsMs)

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.manualBeatsMs.isEmpty())

        viewModel.redo()
        assertEquals(listOf(1_200L), viewModel.currentProject.value!!.manualBeatsMs)
    }

    @Test
    fun `unpin removes only the pin near the playhead`() {
        val viewModel = viewModel(manual = listOf(1_000L, 3_000L))

        assertEquals("Beat removed from 0:01.000", viewModel.unpinBeatAt(1_040L))
        assertEquals(listOf(3_000L), viewModel.currentProject.value!!.manualBeatsMs)

        assertEquals("No pinned beat at the playhead", viewModel.unpinBeatAt(2_000L))
        assertEquals(listOf(3_000L), viewModel.currentProject.value!!.manualBeatsMs)
    }

    @Test
    fun `unpin without pins reports it and does not touch history`() {
        val viewModel = viewModel()
        assertEquals("No pinned beats to remove", viewModel.unpinBeatAt(1_000L))
        assertTrue(viewModel.currentProject.value!!.manualBeatsMs.isEmpty())
    }

    @Test
    fun `clear removes every pin and supports undo`() {
        val viewModel = viewModel(manual = listOf(500L, 1_500L, 2_500L))

        assertEquals("Pinned beats cleared", viewModel.clearManualBeats())
        assertTrue(viewModel.currentProject.value!!.manualBeatsMs.isEmpty())

        viewModel.undo()
        assertEquals(
            listOf(500L, 1_500L, 2_500L),
            viewModel.currentProject.value!!.manualBeatsMs
        )
    }

    @Test
    fun `clear without pins reports that there is nothing to clear`() {
        val viewModel = viewModel()
        assertEquals("No pinned beats to clear", viewModel.clearManualBeats())
    }
}
