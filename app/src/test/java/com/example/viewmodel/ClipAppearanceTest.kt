package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.engine.AudioLoudness
import com.example.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Crop, opacity, loudness and pitch controls from the CapCut clip bar. */
class ClipAppearanceTest {

    private fun viewModel(clip: MediaClip): EditorViewModel {
        val repository = ProjectRepository()
        val vm = EditorViewModel(repository = repository)
        val project = repository.createNewProject("Appearance", initialClips = listOf(clip))
        vm.openProject(project)
        vm.selectClip(clip.id)
        return vm
    }

    private fun clip(
        waveform: List<Float> = emptyList(),
        volume: Float = 1f
    ) = MediaClip(
        id = "clip_a",
        title = "A",
        durationMs = 4_000L,
        originalDurationMs = 4_000L,
        waveform = waveform,
        volume = volume
    )

    @Test
    fun `opacity clamps to zero one`() {
        val vm = viewModel(clip())
        vm.setClipOpacity(0.42f)
        assertEquals(0.42f, vm.currentProject.value?.clips?.first()?.opacity ?: -1f, 0.0001f)

        vm.setClipOpacity(4f)
        assertEquals(1f, vm.currentProject.value?.clips?.first()?.opacity ?: -1f, 0.0001f)

        vm.setClipOpacity(-2f)
        assertEquals(0f, vm.currentProject.value?.clips?.first()?.opacity ?: -1f, 0.0001f)
    }

    @Test
    fun `crop transform is absolute and idempotent`() {
        val vm = viewModel(clip())
        vm.setSelectedClipCrop(2.5f, 0.4f, 0.6f)
        val once = vm.currentProject.value?.clips?.first()
        assertEquals(2.5f, once?.scale ?: -1f, 0.0001f)
        assertEquals(0.4f, once?.posX ?: -1f, 0.0001f)
        assertEquals(0.6f, once?.posY ?: -1f, 0.0001f)

        vm.setSelectedClipCrop(2.5f, 0.4f, 0.6f)
        val twice = vm.currentProject.value?.clips?.first()
        assertEquals(2.5f, twice?.scale ?: -1f, 0.0001f)
    }

    @Test
    fun `crop clamps out of range values`() {
        val vm = viewModel(clip())
        vm.setSelectedClipCrop(0.2f, -3f, 9f)
        val updated = vm.currentProject.value?.clips?.first()
        assertEquals(1f, updated?.scale ?: -1f, 0.0001f)
        assertEquals(0f, updated?.posX ?: -1f, 0.0001f)
        assertEquals(1f, updated?.posY ?: -1f, 0.0001f)
    }

    @Test
    fun `reset crop restores the full frame`() {
        val vm = viewModel(clip())
        vm.setSelectedClipCrop(3f, 0.2f, 0.8f)
        vm.resetSelectedClipCrop()
        val reset = vm.currentProject.value?.clips?.first()
        assertEquals(1f, reset?.scale ?: -1f, 0.0001f)
        assertEquals(0.5f, reset?.posX ?: -1f, 0.0001f)
        assertEquals(0.5f, reset?.posY ?: -1f, 0.0001f)
    }

    @Test
    fun `loudness adjustment normalises a quiet clip`() {
        val waveform = List(32) { 0.05f }
        val vm = viewModel(clip(waveform = waveform))

        vm.setClipLoudnessAdjustment(true)

        val updated = vm.currentProject.value?.clips?.first()
        assertTrue(updated?.isLoudnessAdjusted == true)
        assertEquals(AudioLoudness.gainFor(waveform), updated?.volume ?: -1f, 0.0001f)
        assertTrue((updated?.volume ?: 0f) > 1f)
    }

    @Test
    fun `loudness adjustment without a waveform stays off`() {
        val vm = viewModel(clip(waveform = emptyList()))
        vm.setClipLoudnessAdjustment(true)
        val updated = vm.currentProject.value?.clips?.first()
        assertFalse(updated?.isLoudnessAdjusted == true)
        assertEquals(1f, updated?.volume ?: -1f, 0.0001f)
    }

    @Test
    fun `disabling loudness restores unity gain`() {
        val waveform = List(32) { 0.05f }
        val vm = viewModel(clip(waveform = waveform))
        vm.setClipLoudnessAdjustment(true)
        vm.setClipLoudnessAdjustment(false)

        val updated = vm.currentProject.value?.clips?.first()
        assertFalse(updated?.isLoudnessAdjusted == true)
        assertEquals(1f, updated?.volume ?: -1f, 0.0001f)
    }

    @Test
    fun `change audio pitch flips maintain pitch`() {
        val vm = viewModel(clip())
        assertTrue(vm.currentProject.value?.clips?.first()?.maintainPitch == true)

        vm.setClipMaintainPitch(false)
        assertFalse(vm.currentProject.value?.clips?.first()?.maintainPitch == true)

        vm.setClipMaintainPitch(true)
        assertTrue(vm.currentProject.value?.clips?.first()?.maintainPitch == true)
    }
}
