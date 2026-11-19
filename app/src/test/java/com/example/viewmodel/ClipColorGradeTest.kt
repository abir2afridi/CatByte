package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.MediaClip
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The colour grade of a clip: the HSL values, their undo behaviour and the
 * preview-only comparison with the original.
 */
class ClipColorGradeTest {

    private fun viewModel(): Pair<EditorViewModel, VideoProject> {
        val project = VideoProject(
            id = "grade_project",
            title = "Grade",
            clips = listOf(
                MediaClip(
                    id = "video",
                    title = "Video",
                    durationMs = 5_000L,
                    originalDurationMs = 5_000L
                )
            )
        )
        val viewModel = EditorViewModel(repository = ProjectRepository())
        viewModel.openProject(project)
        return viewModel to project
    }

    @Test
    fun `hsl grade is stored on the clip and supports undo redo`() {
        val (viewModel, _) = viewModel()
        viewModel.selectClip("video")

        viewModel.setClipHslGrade(
            "video",
            listOf(HslAdjustment(HslChannel.RED, hue = 20f, saturation = -10f, luminance = 5f))
        )

        val stored = viewModel.currentProject.value!!.clips.single().hslAdjustments
        assertEquals(1, stored.size)
        assertEquals(HslChannel.RED, stored.single().channel)
        assertEquals(20f, stored.single().hue, 0.0001f)
        assertEquals(-10f, stored.single().saturation, 0.0001f)
        assertEquals(5f, stored.single().luminance, 0.0001f)

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.clips.single().hslAdjustments.isEmpty())

        viewModel.redo()
        assertEquals(1, viewModel.currentProject.value!!.clips.single().hslAdjustments.size)
    }

    @Test
    fun `hsl values are clamped and neutral channels are dropped`() {
        val (viewModel, _) = viewModel()
        viewModel.selectClip("video")

        viewModel.setClipHslGrade(
            "video",
            listOf(
                HslAdjustment(HslChannel.RED, hue = 900f, saturation = Float.NaN, luminance = 0f),
                HslAdjustment(HslChannel.BLUE)
            )
        )

        val stored = viewModel.currentProject.value!!.clips.single().hslAdjustments
        assertEquals(1, stored.size)
        assertEquals(HslChannel.RED, stored.single().channel)
        assertEquals(180f, stored.single().hue, 0f)
        assertEquals(0f, stored.single().saturation, 0f)
    }

    @Test
    fun `hsl slider stream collapses into one undo step`() {
        val (viewModel, _) = viewModel()
        viewModel.selectClip("video")

        viewModel.setClipHslGrade("video", listOf(HslAdjustment(HslChannel.RED, hue = 5f)))
        viewModel.setClipHslGrade("video", listOf(HslAdjustment(HslChannel.RED, hue = 10f)))
        viewModel.setClipHslGrade("video", listOf(HslAdjustment(HslChannel.RED, hue = 15f)))

        viewModel.undo()
        assertTrue(viewModel.currentProject.value!!.clips.single().hslAdjustments.isEmpty())
    }

    @Test
    fun `the comparison is a preview state that ends with the selection`() {
        val (viewModel, _) = viewModel()
        viewModel.selectClip("video")
        assertFalse(viewModel.compareOriginal.value)

        viewModel.setCompareOriginal(true)
        assertTrue(viewModel.compareOriginal.value)

        // Selecting another clip ends the comparison: it belonged to the
        // graded clip.
        viewModel.selectClip("other")
        assertFalse(viewModel.compareOriginal.value)
    }

    @Test
    fun `clearing the selection ends the comparison`() {
        val (viewModel, _) = viewModel()
        viewModel.selectClip("video")
        viewModel.setCompareOriginal(true)

        viewModel.clearSelection()

        assertFalse(viewModel.compareOriginal.value)
    }

    @Test
    fun `leaving the adjust panel ends the comparison`() {
        val (viewModel, _) = viewModel()
        viewModel.selectClip("video")
        viewModel.setActiveSheet(ActiveToolSheet.ADJUSTMENTS)
        viewModel.setCompareOriginal(true)

        viewModel.setActiveSheet(ActiveToolSheet.NONE)

        assertFalse(viewModel.compareOriginal.value)
    }

    @Test
    fun `opening another project ends the comparison`() {
        val (viewModel, project) = viewModel()
        viewModel.selectClip("video")
        viewModel.setCompareOriginal(true)

        viewModel.openProject(project.copy(id = "other_project"))

        assertFalse(viewModel.compareOriginal.value)
    }
}
