package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Marker management UI (rename / recolour / delete / jump).
 *
 * The ruler already drew the pins and snapping already used them; the chips
 * only toggled a counter. These tests pin the model-level behaviour behind the
 * new sheet.
 */
class MarkerFeatureTest {

    private fun viewModelWithClip(): EditorViewModel {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val clip = MediaClip(
            id = "clip_1",
            title = "First",
            durationMs = 5_000L,
            originalDurationMs = 5_000L
        )
        val project = ProjectRepository().createNewProject("Markers", initialClips = listOf(clip))
        viewModel.openProject(project)
        return viewModel
    }

    @Test
    fun `add places a marker at the playhead`() {
        val viewModel = viewModelWithClip()
        viewModel.seekTo(1_500L)

        viewModel.addMarker("Hook")

        val marker = viewModel.currentProject.value!!.markers.single()
        assertEquals(1_500L, marker.atMs)
        assertEquals("Hook", marker.label)
    }

    @Test
    fun `rename updates only the target marker`() {
        val viewModel = viewModelWithClip()
        viewModel.addMarker("first")
        viewModel.seekTo(2_000L)
        viewModel.addMarker("second")

        val markers = viewModel.currentProject.value!!.markers
        val first = markers.first { it.label == "first" }
        viewModel.renameMarker(first.id, "Intro")

        val updated = viewModel.currentProject.value!!.markers
        assertEquals("Intro", updated.first { it.id == first.id }.label)
        assertEquals("second", updated.first { it.label == "second" }.label)
    }

    @Test
    fun `marker can move to the current playhead and undo restores its position`() {
        val viewModel = viewModelWithClip()
        viewModel.addMarker("Hook")
        val id = viewModel.currentProject.value!!.markers.single().id
        viewModel.seekTo(2_250L)

        viewModel.moveMarkerToPlayhead(id)
        assertEquals(2_250L, viewModel.currentProject.value!!.markers.single().atMs)

        viewModel.undo()
        assertEquals(0L, viewModel.currentProject.value!!.markers.single().atMs)
    }

    @Test
    fun `rename is capped so the ruler label stays readable`() {
        val viewModel = viewModelWithClip()
        viewModel.addMarker()
        val id = viewModel.currentProject.value!!.markers.single().id

        viewModel.renameMarker(id, "x".repeat(80))

        assertEquals(24, viewModel.currentProject.value!!.markers.single().label.length)
    }

    @Test
    fun `recolour updates the pin colour`() {
        val viewModel = viewModelWithClip()
        viewModel.addMarker()
        val id = viewModel.currentProject.value!!.markers.single().id

        viewModel.setMarkerColor(id, 0xFF26E5D7)

        assertEquals(0xFF26E5D7, viewModel.currentProject.value!!.markers.single().colorArgb)
    }

    @Test
    fun `delete removes the marker`() {
        val viewModel = viewModelWithClip()
        viewModel.addMarker()
        val id = viewModel.currentProject.value!!.markers.single().id

        viewModel.removeMarker(id)

        assertTrue(viewModel.currentProject.value!!.markers.isEmpty())
    }

    @Test
    fun `marker edits are undoable`() {
        val viewModel = viewModelWithClip()
        viewModel.addMarker("Hook")
        val id = viewModel.currentProject.value!!.markers.single().id
        viewModel.renameMarker(id, "Chorus")

        viewModel.undo()

        assertEquals("Hook", viewModel.currentProject.value!!.markers.single().label)
    }
}
