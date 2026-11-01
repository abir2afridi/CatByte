package com.example.viewmodel

import com.example.model.AspectRatioType
import com.example.data.ProjectRepository
import com.example.model.MediaClip
import com.example.model.VideoFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Editor state machine without Android media plumbing: repository is in
 * memory (no Context) and playback/export managers are left null.
 */
class EditorWorkflowTest {

    private fun newViewModel(): EditorViewModel {
        val repo = ProjectRepository()
        return EditorViewModel(repository = repo)
    }

    private fun vmWithOneClip(): EditorViewModel {
        val viewModel = newViewModel()
        val clip = MediaClip(id = "clip_1", title = "First", durationMs = 3000L, originalDurationMs = 3000L)
        val repo = ProjectRepository()
        val project = repo.createNewProject("Seeded", initialClips = listOf(clip))
        viewModel.openProject(project)
        return viewModel
    }

    @Test
    fun `empty repository starts without a current project`() {
        val viewModel = newViewModel()
        assertNull(viewModel.currentProject.value)
    }

    @Test
    fun `createNewProjectWithParams sets title ratio and paper style`() {
        val viewModel = newViewModel()
        viewModel.createNewProjectWithParams("Blueprint Test", AspectRatioType.RATIO_16_9, "Standard")

        val current = viewModel.currentProject.value
        assertNotNull(current)
        assertEquals("Blueprint Test", current?.title)
        assertEquals(AspectRatioType.RATIO_16_9, current?.aspectRatio)
        assertEquals("Standard", current?.paperStyle)
        assertTrue(viewModel.projects.value.any { it.id == current?.id })
    }

    @Test
    fun `setAspectRatio is undoable`() {
        val viewModel = vmWithOneClip()
        viewModel.setAspectRatio(AspectRatioType.RATIO_16_9)
        assertEquals(AspectRatioType.RATIO_16_9, viewModel.currentProject.value?.aspectRatio)

        viewModel.undo()
        assertEquals(AspectRatioType.RATIO_9_16, viewModel.currentProject.value?.aspectRatio)

        viewModel.redo()
        assertEquals(AspectRatioType.RATIO_16_9, viewModel.currentProject.value?.aspectRatio)
    }

    @Test
    fun `undo and redo flags follow the stacks`() {
        val viewModel = vmWithOneClip()
        assertFalse(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        viewModel.setAspectRatio(AspectRatioType.RATIO_1_1)
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        viewModel.undo()
        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)

        viewModel.redo()
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
    }

    @Test
    fun `undo with empty history is a no-op`() {
        val viewModel = vmWithOneClip()
        val before = viewModel.currentProject.value
        viewModel.undo()
        assertEquals(before, viewModel.currentProject.value)
        viewModel.redo()
        assertEquals(before, viewModel.currentProject.value)
    }

    @Test
    fun `split at playhead divides the selected clip`() {
        val viewModel = vmWithOneClip()
        viewModel.seekTo(1200L)
        viewModel.splitClipAtPlayhead()

        val clips = viewModel.currentProject.value?.clips.orEmpty()
        assertEquals(2, clips.size)
        // Part 1: 0..1200ms, Part 2: 1200..3000ms
        assertEquals(1200L, clips[0].durationMs)
        assertEquals(1800L, clips[1].durationMs)
        assertEquals(3000L, clips.sumOf { it.durationMs })
    }

    @Test
    fun `split inside the first 100ms does nothing`() {
        val viewModel = vmWithOneClip()
        viewModel.seekTo(50L)
        viewModel.splitClipAtPlayhead()
        assertEquals(1, viewModel.currentProject.value?.clips?.size)
    }

    @Test
    fun `delete removes only the selected clip`() {
        val viewModel = vmWithOneClip()
        val first = viewModel.currentProject.value?.clips?.first()
        viewModel.selectClip(first?.id)
        viewModel.deleteSelectedClip()
        assertTrue(viewModel.currentProject.value?.clips.isNullOrEmpty())
    }

    @Test
    fun `filter change lands on the selected clip only`() {
        val viewModel = vmWithOneClip()
        val clipId = viewModel.currentProject.value?.clips?.firstOrNull()?.id
        viewModel.selectClip(clipId)
        viewModel.setClipFilter(VideoFilter.CYBERPUNK)
        assertEquals(VideoFilter.CYBERPUNK, viewModel.currentProject.value?.clips?.find { it.id == clipId }?.filter)
    }

    @Test
    fun `history is capped at 25 entries`() {
        val viewModel = vmWithOneClip()
        repeat(40) { index ->
            viewModel.setAspectRatio(if (index % 2 == 0) AspectRatioType.RATIO_16_9 else AspectRatioType.RATIO_9_16)
        }
        // 40 pushes, stack keeps 25 -> undo until exhausted.
        var undos = 0
        while (viewModel.canUndo.value && undos < 100) {
            viewModel.undo()
            undos++
        }
        assertEquals(25, undos)
        assertFalse(viewModel.canUndo.value)
    }

    @Test
    fun `setTimelineZoom is clamped between half and triple`() {
        val viewModel = vmWithOneClip()
        viewModel.setTimelineZoom(500f)
        assertEquals(3.0f, viewModel.timelineZoom.value, 0.0001f)
        viewModel.setTimelineZoom(0.001f)
        assertEquals(0.5f, viewModel.timelineZoom.value, 0.0001f)
    }

    @Test
    fun `magnetic snap toggles`() {
        val viewModel = vmWithOneClip()
        assertTrue(viewModel.magneticSnap.value)
        viewModel.toggleMagneticSnap()
        assertFalse(viewModel.magneticSnap.value)
        viewModel.toggleMagneticSnap()
        assertTrue(viewModel.magneticSnap.value)
    }

    @Test
    fun `selecting text clears clip selection and vice versa`() {
        val viewModel = vmWithOneClip()
        viewModel.selectClip("clip_1")
        viewModel.selectText("text_1")
        assertNull(viewModel.selectedClipId.value)
        viewModel.selectClip("clip_1")
        assertNull(viewModel.selectedTextId.value)
    }
}
