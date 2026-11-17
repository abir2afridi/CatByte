package com.example.viewmodel

import com.example.data.KeyframeInterpolator
import com.example.data.ProjectRepository
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text overlay keyframes: the renderers already evaluated them, these tests pin
 * the new authoring path (add at playhead, replace near-duplicates, remove).
 */
class TextKeyframeTest {

    private fun viewModel(): EditorViewModel {
        val repository = ProjectRepository()
        val clip = MediaClip(
            id = "clip_1",
            title = "First",
            durationMs = 10_000L,
            originalDurationMs = 10_000L
        )
        val viewModel = EditorViewModel(repository = repository)
        viewModel.openProject(repository.createNewProject("Text keys", initialClips = listOf(clip)))
        return viewModel
    }

    private fun EditorViewModel.selectedKeys(property: KeyframeProperty? = null) =
        currentProject.value!!.textOverlays.single().keyframes
            .filter { property == null || it.property == property }
            .sortedBy { it.timestampMs }

    @Test
    fun `keyframe is added at the playhead inside the overlay`() {
        val viewModel = viewModel()
        viewModel.seekTo(0L)
        viewModel.addTextOverlay("Hello")
        viewModel.seekTo(1_000L)

        assertTrue(viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1.5f))

        val key = viewModel.selectedKeys().single()
        assertEquals(1_000L, key.timestampMs)
        assertEquals(1.5f, key.value, 0.001f)
    }

    @Test
    fun `two keys animate and interpolate between them`() {
        val viewModel = viewModel()
        viewModel.seekTo(0L)
        viewModel.addTextOverlay("Hello")
        viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1f)
        viewModel.seekTo(2_000L)
        viewModel.setTextKeyframe(KeyframeProperty.SCALE, 2f)

        val keys = viewModel.selectedKeys(KeyframeProperty.SCALE)
        assertEquals(2, keys.size)
        assertEquals(
            1.5f,
            KeyframeInterpolator.interpolate(keys, KeyframeProperty.SCALE, 1_000L, 1f),
            0.001f
        )
    }

    @Test
    fun `adding again near the same time replaces instead of duplicating`() {
        val viewModel = viewModel()
        viewModel.seekTo(0L)
        viewModel.addTextOverlay("Hello")
        viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1.2f)
        viewModel.seekTo(1_000L)
        viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1.4f)
        viewModel.setTextKeyframe(KeyframeProperty.SCALE, 2.2f)

        val keys = viewModel.selectedKeys(KeyframeProperty.SCALE)
        assertEquals(2, keys.size)
        assertEquals(2.2f, keys.last().value, 0.001f)
    }

    @Test
    fun `different properties keep independent keys`() {
        val viewModel = viewModel()
        viewModel.seekTo(0L)
        viewModel.addTextOverlay("Hello")
        viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1.2f)
        viewModel.setTextKeyframe(KeyframeProperty.POSITION_X, 0.7f)
        viewModel.setTextKeyframe(KeyframeProperty.OPACITY, 0.5f)

        assertEquals(3, viewModel.selectedKeys().size)
    }

    @Test
    fun `playhead outside the overlay is refused`() {
        val viewModel = viewModel()
        viewModel.seekTo(0L)
        viewModel.addTextOverlay("Hello") // 0..3000ms
        viewModel.seekTo(5_000L)

        assertFalse(viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1.5f))
        assertTrue(viewModel.selectedKeys().isEmpty())
    }

    @Test
    fun `without a selected text nothing is added`() {
        val viewModel = viewModel()
        viewModel.seekTo(1_000L)

        assertFalse(viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1.5f))
    }

    @Test
    fun `remove and clear delete only text keys`() {
        val viewModel = viewModel()
        viewModel.seekTo(0L)
        viewModel.addTextOverlay("Hello")
        viewModel.setTextKeyframe(KeyframeProperty.SCALE, 1f)
        viewModel.seekTo(1_000L)
        viewModel.setTextKeyframe(KeyframeProperty.POSITION_X, 0.5f)
        viewModel.seekTo(2_000L)
        viewModel.setTextKeyframe(KeyframeProperty.OPACITY, 0.8f)

        val scaleKey = viewModel.selectedKeys(KeyframeProperty.SCALE).single()
        viewModel.removeTextKeyframe(scaleKey.id)
        assertEquals(2, viewModel.selectedKeys().size)
        assertNotNull(viewModel.currentProject.value!!.textOverlays.single())

        viewModel.clearTextKeyframes()
        assertTrue(viewModel.selectedKeys().isEmpty())
    }
}
