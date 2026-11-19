package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.engine.AudioLoudness
import com.example.engine.ClipAnimationMath
import com.example.model.AudioEffectSettings
import com.example.model.MediaClip
import com.example.model.VoiceEffectType
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
    fun `rotation is stored on the clip and normalised to a full turn`() {
        val vm = viewModel(clip())
        vm.setSelectedClipRotation(90f)
        assertEquals(90f, vm.currentProject.value?.clips?.first()?.rotation ?: -1f, 0.0001f)

        vm.setSelectedClipRotation(-90f)
        assertEquals(270f, vm.currentProject.value?.clips?.first()?.rotation ?: -1f, 0.0001f)

        vm.setSelectedClipRotation(450f)
        assertEquals(90f, vm.currentProject.value?.clips?.first()?.rotation ?: -1f, 0.0001f)
    }

    @Test
    fun `a non finite rotation falls back to zero`() {
        val vm = viewModel(clip())
        vm.setSelectedClipRotation(90f)
        vm.setSelectedClipRotation(Float.NaN)
        assertEquals(0f, vm.currentProject.value?.clips?.first()?.rotation ?: -1f, 0.0001f)
    }

    @Test
    fun `the mirror flag is stored on the clip and undoes`() {
        val vm = viewModel(clip())
        assertFalse(vm.currentProject.value?.clips?.first()?.isMirrored == true)

        vm.setSelectedClipMirrored(true)
        assertTrue(vm.currentProject.value?.clips?.first()?.isMirrored == true)

        vm.undo()
        assertFalse(vm.currentProject.value?.clips?.first()?.isMirrored == true)

        vm.redo()
        assertTrue(vm.currentProject.value?.clips?.first()?.isMirrored == true)
    }

    @Test
    fun `the animation duration is stored on the clip and clamped`() {
        val vm = viewModel(clip())
        vm.setClipAnimationDuration(1_500L)
        assertEquals(1_500L, vm.currentProject.value?.clips?.first()?.animationDurationMs ?: -1L)

        vm.setClipAnimationDuration(900_000L)
        assertEquals(
            ClipAnimationMath.MAX_DURATION_MS,
            vm.currentProject.value?.clips?.first()?.animationDurationMs ?: -1L
        )

        vm.setClipAnimationDuration(1L)
        assertEquals(
            ClipAnimationMath.MIN_DURATION_MS,
            vm.currentProject.value?.clips?.first()?.animationDurationMs ?: -1L
        )
    }

    @Test
    fun `a rapid animation duration drag collapses to one undo step`() {
        val vm = viewModel(clip())
        vm.setClipAnimationDuration(400L)
        vm.setClipAnimationDuration(900L)
        vm.setClipAnimationDuration(1_400L)

        vm.undo()

        assertEquals(
            ClipAnimationMath.DURATION_MS,
            vm.currentProject.value?.clips?.first()?.animationDurationMs ?: -1L
        )
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

    @Test
    fun `clip audio fades are stored on the output timeline`() {
        val vm = viewModel(clip())

        vm.setClipAudioFades(600L, 900L)

        val updated = vm.currentProject.value?.clips?.first()
        assertEquals(600L, updated?.audioFadeInMs ?: -1L)
        assertEquals(900L, updated?.audioFadeOutMs ?: -1L)
    }

    @Test
    fun `overlapping clip fades are scaled to fit the clip`() {
        val vm = viewModel(clip()) // 4 s long

        vm.setClipAudioFades(3_000L, 3_000L)

        val updated = vm.currentProject.value?.clips?.first()
        val fadeIn = updated?.audioFadeInMs ?: -1L
        val fadeOut = updated?.audioFadeOutMs ?: -1L
        // Scaled proportionally, so both fades together fill the clip.
        assertEquals(4_000L, fadeIn + fadeOut)
        assertEquals(2_000L, fadeIn)
    }

    @Test
    fun `clip fades are clamped to the clip duration and undo as one step`() {
        val vm = viewModel(clip())

        vm.setClipAudioFades(9_000L, 9_000L)

        var updated = vm.currentProject.value?.clips?.first()
        assertEquals(2_000L, updated?.audioFadeInMs ?: -1L)
        assertEquals(2_000L, updated?.audioFadeOutMs ?: -1L)

        vm.undo()
        updated = vm.currentProject.value?.clips?.first()
        assertEquals(0L, updated?.audioFadeInMs ?: -1L)
        assertEquals(0L, updated?.audioFadeOutMs ?: -1L)
    }

    @Test
    fun `clip audio effects persist and undo`() {
        val vm = viewModel(clip())
        val effects = AudioEffectSettings(
            type = VoiceEffectType.REVERB,
            intensity = 0.5f,
            enhanceVoice = true
        )

        vm.setClipAudioEffects(effects)

        val updated = vm.currentProject.value?.clips?.first()
        assertEquals(VoiceEffectType.REVERB, updated?.audioEffects?.type)
        assertTrue(updated?.audioEffects?.enhanceVoice == true)

        vm.undo()
        assertTrue(vm.currentProject.value?.clips?.first()?.audioEffects?.isNoOp == true)
    }
}
