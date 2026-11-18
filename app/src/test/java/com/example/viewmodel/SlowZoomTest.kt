package com.example.viewmodel

import com.example.data.KeyframeInterpolator
import com.example.data.ProjectRepository
import com.example.engine.KenBurns
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
import com.example.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Slow zoom (Ken Burns) toggled on the selected clip. */
class SlowZoomTest {

    private fun project(clip: MediaClip) = VideoProject(
        id = "slow_zoom",
        title = "Slow zoom",
        clips = listOf(clip)
    )

    private fun clip(
        durationMs: Long = 4_000L,
        keyframes: List<Keyframe> = emptyList(),
        scale: Float = 1f
    ) = MediaClip(
        id = "clip",
        title = "Clip",
        durationMs = durationMs,
        originalDurationMs = durationMs,
        keyframes = keyframes,
        scale = scale
    )

    private fun viewModel(project: VideoProject): EditorViewModel {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        viewModel.openProject(project)
        return viewModel
    }

    @Test
    fun `the toggle adds a real scale ramp and removes it again`() {
        val viewModel = viewModel(project(clip()))
        viewModel.selectClip("clip")

        assertEquals("Slow zoom added (1.00x to 1.12x)", viewModel.toggleKenBurns())
        val keyframes = viewModel.currentProject.value!!.clips.single().keyframes
        assertEquals(2, keyframes.size)
        assertTrue(keyframes.all { it.property == KeyframeProperty.SCALE })
        assertEquals(1f, KeyframeInterpolator.getScale(
            viewModel.currentProject.value!!.clips.single(), 0L
        ), 0.0001f)
        assertEquals(KenBurns.END_SCALE, KeyframeInterpolator.getScale(
            viewModel.currentProject.value!!.clips.single(), 4_000L
        ), 0.0001f)

        assertEquals("Slow zoom removed", viewModel.toggleKenBurns())
        assertTrue(viewModel.currentProject.value!!.clips.single().keyframes.isEmpty())
    }

    @Test
    fun `removing the zoom is undoable and redoable`() {
        val viewModel = viewModel(project(clip()))
        viewModel.selectClip("clip")
        viewModel.toggleKenBurns()
        viewModel.toggleKenBurns()

        viewModel.undo()
        assertEquals(2, viewModel.currentProject.value!!.clips.single().keyframes.size)

        viewModel.redo()
        assertTrue(viewModel.currentProject.value!!.clips.single().keyframes.isEmpty())
    }

    @Test
    fun `a manually scaled clip keeps its framing`() {
        val viewModel = viewModel(project(clip(scale = 1.3f)))
        viewModel.selectClip("clip")

        assertEquals("Slow zoom added (1.30x to 1.46x)", viewModel.toggleKenBurns())

        val scaled = viewModel.currentProject.value!!.clips.single()
        assertEquals(1.3f, KeyframeInterpolator.getScale(scaled, 0L), 0.0001f)
        assertEquals(1.3f * 1.12f, KeyframeInterpolator.getScale(scaled, 4_000L), 0.0001f)
        assertEquals("Slow zoom removed", viewModel.toggleKenBurns())
    }

    @Test
    fun `existing transform keyframes block the automatic move`() {
        val existing = Keyframe(
            timestampMs = 1_000L,
            property = KeyframeProperty.POSITION_X,
            value = 0.3f
        )
        val viewModel = viewModel(project(clip(keyframes = listOf(existing))))
        viewModel.selectClip("clip")

        assertEquals(
            "Remove the existing scale/position keyframes first",
            viewModel.toggleKenBurns()
        )
        assertEquals(
            listOf(existing),
            viewModel.currentProject.value!!.clips.single().keyframes
        )
    }

    @Test
    fun `a clip shorter than the minimum is refused honestly`() {
        val viewModel = viewModel(project(clip(durationMs = 300L)))
        viewModel.selectClip("clip")

        assertEquals(
            "This clip is too short for a slow zoom",
            viewModel.toggleKenBurns()
        )
        assertTrue(viewModel.currentProject.value!!.clips.single().keyframes.isEmpty())
    }

    @Test
    fun `without a selected clip nothing is changed`() {
        val viewModel = viewModel(project(clip()))

        assertEquals("Select a clip first", viewModel.toggleKenBurns())
        assertNull(viewModel.currentProject.value!!.clips.single().keyframes.firstOrNull())
    }
}
