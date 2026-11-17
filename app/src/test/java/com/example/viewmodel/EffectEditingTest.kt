package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.EffectFamily
import com.example.model.EffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Tap an effect chip on the timeline": editing a placed effect's duration and
 * intensity, and toggling GPU motion blur on a clip.
 */
class EffectEditingTest {

    private fun viewModelWithEffect(): Pair<EditorViewModel, String> {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val project = ProjectRepository().createNewProject("Effects")
        viewModel.openProject(project)
        viewModel.seekTo(1_000L)
        viewModel.addTimelineEffect(EffectType.entries.first { it.spec.family == EffectFamily.PARTICLES })
        val id = viewModel.currentProject.value!!.effects.single().id
        return viewModel to id
    }

    @Test
    fun `editing duration and intensity updates the placed effect`() {
        val (viewModel, id) = viewModelWithEffect()

        viewModel.updateTimelineEffect(id, durationMs = 4_000L, intensityScale = 1.5f)

        val effect = viewModel.currentProject.value!!.effects.single()
        assertEquals(4_000L, effect.durationMs)
        assertEquals(1.5f, effect.intensityScale, 0.001f)
    }

    @Test
    fun `edits are clamped to sane ranges`() {
        val (viewModel, id) = viewModelWithEffect()

        viewModel.updateTimelineEffect(id, durationMs = 5L, intensityScale = 9f)

        val effect = viewModel.currentProject.value!!.effects.single()
        assertEquals(300L, effect.durationMs)
        assertEquals(2f, effect.intensityScale, 0.001f)
    }

    @Test
    fun `editing only touches the targeted effect`() {
        val (viewModel, id) = viewModelWithEffect()
        viewModel.seekTo(2_500L)
        viewModel.addTimelineEffect(EffectType.entries.first { it.spec.family == EffectFamily.STARFIELD })

        viewModel.updateTimelineEffect(id, durationMs = 6_000L, intensityScale = 0.5f)

        val effects = viewModel.currentProject.value!!.effects
        assertEquals(2, effects.size)
        assertEquals(6_000L, effects.first { it.id == id }.durationMs)
        assertEquals(2_500L, effects.first { it.id != id }.startTimelineMs)
    }

    @Test
    fun `deleting removes the effect`() {
        val (viewModel, id) = viewModelWithEffect()

        viewModel.deleteTimelineEffect(id)

        assertTrue(viewModel.currentProject.value!!.effects.isEmpty())
    }

    @Test
    fun `motion blur toggles on the selected clip`() {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val clip = com.example.model.MediaClip(
            id = "clip_1",
            title = "First",
            durationMs = 6_000L,
            originalDurationMs = 6_000L
        )
        viewModel.openProject(ProjectRepository().createNewProject("Blur", initialClips = listOf(clip)))
        viewModel.selectClip("clip_1")

        assertTrue(viewModel.toggleMotionBlurOnSelectedClip(true))
        assertTrue(viewModel.currentProject.value!!.clips.single().motionBlur)

        // Second call with the same value is a no-op that still succeeds.
        assertTrue(viewModel.toggleMotionBlurOnSelectedClip(true))

        assertTrue(viewModel.toggleMotionBlurOnSelectedClip(false))
        assertTrue(!viewModel.currentProject.value!!.clips.single().motionBlur)
    }

    @Test
    fun `motion blur without a selection is rejected`() {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val project = ProjectRepository().createNewProject("Blur")
        viewModel.openProject(project)

        assertNull(viewModel.selectedClipId.value)
        assertTrue(!viewModel.toggleMotionBlurOnSelectedClip(true))
    }
}
