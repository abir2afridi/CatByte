package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.EffectFamily
import com.example.model.EffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The effect panel's "None" card removes the effect the playhead is on.
 * These tests pin that the effect is really found and really removed.
 */
class TimelineEffectRemovalTest {

    private fun viewModel(): EditorViewModel {
        val vm = EditorViewModel(repository = ProjectRepository())
        vm.openProject(ProjectRepository().createNewProject("Effects"))
        return vm
    }

    private fun preset() = EffectType.entries.first { it.spec.family == EffectFamily.PARTICLES }

    @Test
    fun `an effect at the playhead is found`() {
        val vm = viewModel()
        vm.seekTo(1_000L)
        vm.addTimelineEffect(preset())

        assertTrue(vm.hasEffectAtPlayhead())
    }

    @Test
    fun `without an effect there is nothing to remove`() {
        val vm = viewModel()
        vm.seekTo(1_000L)

        assertFalse(vm.hasEffectAtPlayhead())
    }

    @Test
    fun `removing deletes the effect under the playhead`() {
        val vm = viewModel()
        vm.seekTo(1_000L)
        vm.addTimelineEffect(preset())

        vm.removeEffectAtPlayhead()

        assertTrue(vm.currentProject.value?.effects?.isEmpty() == true)
        assertFalse(vm.hasEffectAtPlayhead())
    }

    @Test
    fun `an effect that ended before the playhead is not removed`() {
        val vm = viewModel()
        vm.seekTo(1_000L)
        vm.addTimelineEffect(preset())
        // The effect lasts 2.5 s from the playhead.
        vm._currentPositionMs.value = 5_000L

        assertFalse(vm.hasEffectAtPlayhead())

        vm.removeEffectAtPlayhead()

        assertEquals(1, vm.currentProject.value?.effects?.size ?: 0)
    }

    @Test
    fun `only the effect under the playhead is removed`() {
        val vm = viewModel()
        vm.seekTo(1_000L)
        vm.addTimelineEffect(preset())
        // The playhead is set directly: seekTo() clamps to the project
        // length, which the placed effect itself stretches.
        vm._currentPositionMs.value = 5_000L
        vm.addTimelineEffect(
            EffectType.entries.first { it.spec.family == EffectFamily.STARFIELD }
        )

        vm._currentPositionMs.value = 1_500L
        vm.removeEffectAtPlayhead()

        val effects = vm.currentProject.value?.effects.orEmpty()
        assertEquals(1, effects.size)
        assertEquals(5_000L, effects.single().startTimelineMs)
    }
}
