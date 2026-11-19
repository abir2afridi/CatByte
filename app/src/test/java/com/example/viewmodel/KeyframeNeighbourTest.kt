package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The keyframe navigation buttons need to know which keyframe sits before
 * and after the playhead. The keyframes live in clip-local time, so the
 * lookup has to map them back onto the project timeline.
 */
class KeyframeNeighbourTest {

    private fun viewModel(clips: List<MediaClip>): EditorViewModel {
        val repository = ProjectRepository()
        val vm = EditorViewModel(repository = repository)
        vm.openProject(repository.createNewProject("Keys", initialClips = clips))
        return vm
    }

    private fun clip(id: String, durationMs: Long, keyframes: List<Keyframe>) = MediaClip(
        id = id,
        title = id,
        durationMs = durationMs,
        originalDurationMs = durationMs,
        keyframes = keyframes
    )

    private fun key(timestampMs: Long) = Keyframe(
        id = "k_$timestampMs",
        timestampMs = timestampMs,
        property = KeyframeProperty.SCALE,
        value = 1f,
        easing = EasingCurve.LINEAR
    )

    @Test
    fun `the neighbours are mapped onto the project timeline`() {
        val vm = viewModel(
            listOf(
                clip("a", 2_000L, emptyList()),
                clip("b", 4_000L, listOf(key(500L), key(2_500L)))
            )
        )

        // The second clip starts at 2 s on the timeline, so the playhead at
        // 3 s is 1 s into that clip: 500 ms is behind it and 2.5 s ahead.
        val (previous, next) = vm.keyframeNeighbours("b", 3_000L)

        assertEquals(2_500L, previous)
        assertEquals(4_500L, next)
    }

    @Test
    fun `there is no next keyframe behind the last one`() {
        val vm = viewModel(
            listOf(
                clip("a", 2_000L, emptyList()),
                clip("b", 4_000L, listOf(key(500L), key(2_500L)))
            )
        )

        val (previous, next) = vm.keyframeNeighbours("b", 5_500L)

        assertEquals(4_500L, previous)
        assertNull(next)
    }

    @Test
    fun `the next keyframe is found after the playhead`() {
        val vm = viewModel(
            listOf(clip("a", 4_000L, listOf(key(500L), key(2_500L))))
        )

        val (previous, next) = vm.keyframeNeighbours("a", 1_000L)

        assertEquals(500L, previous)
        assertEquals(2_500L, next)
    }

    @Test
    fun `a clip without keyframes has no neighbours`() {
        val vm = viewModel(listOf(clip("a", 4_000L, emptyList())))

        val (previous, next) = vm.keyframeNeighbours("a", 1_000L)

        assertNull(previous)
        assertNull(next)
    }

    @Test
    fun `an unknown clip has no neighbours`() {
        val vm = viewModel(listOf(clip("a", 4_000L, listOf(key(500L)))))

        val (previous, next) = vm.keyframeNeighbours("missing", 1_000L)

        assertNull(previous)
        assertNull(next)
    }

    @Test
    fun `the playhead exactly on a keyframe skips it`() {
        val vm = viewModel(listOf(clip("a", 4_000L, listOf(key(1_000L), key(2_000L)))))

        val (previous, next) = vm.keyframeNeighbours("a", 1_000L)

        assertNull(previous)
        assertEquals(2_000L, next)
    }
}
