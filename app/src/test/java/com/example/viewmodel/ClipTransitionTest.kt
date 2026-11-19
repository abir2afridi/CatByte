package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.MediaClip
import com.example.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The transition picker can be opened for a clip that is not the selected
 * one, so the transition must land on the clip the picker was opened for -
 * never on whatever happens to be selected.
 */
class ClipTransitionTest {

    private fun viewModel(clips: List<MediaClip>): EditorViewModel {
        val repository = ProjectRepository()
        val vm = EditorViewModel(repository = repository)
        vm.openProject(repository.createNewProject("Transitions", initialClips = clips))
        return vm
    }

    private fun clip(id: String) = MediaClip(
        id = id,
        title = id,
        durationMs = 4_000L,
        originalDurationMs = 4_000L
    )

    @Test
    fun `the transition lands on the clip the picker was opened for`() {
        val vm = viewModel(listOf(clip("a"), clip("b")))
        vm.selectClip("a")

        vm.setClipTransitionForClip("b", TransitionType.PAGE_FLIP)

        val clips = vm.currentProject.value?.clips.orEmpty()
        assertEquals(TransitionType.NONE, clips.first { it.id == "a" }.transition)
        assertEquals(TransitionType.PAGE_FLIP, clips.first { it.id == "b" }.transition)
    }

    @Test
    fun `the transition length lands on the clip the picker was opened for`() {
        val vm = viewModel(listOf(clip("a"), clip("b")))
        vm.selectClip("a")

        vm.setClipTransitionDuration("b", 1_200L)

        val clips = vm.currentProject.value?.clips.orEmpty()
        assertEquals(400L, clips.first { it.id == "a" }.transitionDurationMs)
        assertEquals(1_200L, clips.first { it.id == "b" }.transitionDurationMs)
    }

    @Test
    fun `the transition length is clamped into the supported range`() {
        val vm = viewModel(listOf(clip("a")))

        vm.setClipTransitionDuration("a", 900_000L)
        assertEquals(
            TransitionType.MAX_DURATION_MS,
            vm.currentProject.value?.clips?.first()?.transitionDurationMs ?: -1L
        )

        vm.setClipTransitionDuration("a", 1L)
        assertEquals(
            TransitionType.MIN_DURATION_MS,
            vm.currentProject.value?.clips?.first()?.transitionDurationMs ?: -1L
        )
    }

    @Test
    fun `a rapid transition length drag collapses to one undo step`() {
        val vm = viewModel(listOf(clip("a")))

        vm.setClipTransitionDuration("a", 600L)
        vm.setClipTransitionDuration("a", 900L)
        vm.setClipTransitionDuration("a", 1_400L)

        vm.undo()

        assertEquals(
            400L,
            vm.currentProject.value?.clips?.first()?.transitionDurationMs ?: -1L
        )
    }

    @Test
    fun `an unknown clip id changes nothing`() {
        val vm = viewModel(listOf(clip("a")))

        vm.setClipTransitionForClip("", TransitionType.GLITCH)
        vm.setClipTransitionDuration("missing", 900L)

        val clip = vm.currentProject.value?.clips?.first()
        assertEquals(TransitionType.NONE, clip?.transition)
        assertEquals(400L, clip?.transitionDurationMs ?: -1L)
        assertTrue(clip != null)
    }
}
