package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.AspectRatioType
import com.example.model.BlendMode
import com.example.model.ClipAnimation
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TransitionType
import com.example.model.VideoFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Plan §12 gate: property-based fuzz of "random edit sequences, undo-all must
 * equal original". Runs the real [EditorViewModel] state machine (pure JVM:
 * in-memory repository, no media plumbing) over seeded random edit streams and
 * proves the snapshot history survives every path and drains exactly.
 */
class UndoFuzzTest {

    /** Fresh project with two clips; deeper covers all mutation paths. */
    private fun seededViewModel(seed: Long): EditorViewModel {
        val repo = ProjectRepository()
        val project = repo.createNewProject(
            title = "Fuzz $seed",
            initialClips = listOf(
                MediaClip(id = "clip_1", title = "One", durationMs = 3000L, originalDurationMs = 3000L),
                MediaClip(id = "clip_2", title = "Two", durationMs = 2500L, originalDurationMs = 2500L)
            )
        )
        val viewModel = EditorViewModel(repository = repo)
        viewModel.openProject(project)
        return viewModel
    }

    /** Applies one random but *definitely mutating* editor action. */
    private fun applyRandomOp(viewModel: EditorViewModel, rng: Random) {
        val project = viewModel.currentProject.value ?: return
        val clip = (project.clips + project.overlayTracks.flatMap { it.clips })
            .randomOrNull(rng) ?: return
        when (rng.nextInt(20)) {
            0 -> viewModel.selectClip(clip.id).also { viewModel.setClipFilter(VideoFilter.entries.random(rng)) }
            1 -> viewModel.selectClip(clip.id).also { viewModel.setClipTransition(TransitionType.entries.random(rng)) }
            2 -> viewModel.selectClip(clip.id).also { viewModel.setClipAnimation(ClipAnimation.entries.random(rng)) }
            3 -> viewModel.selectClip(clip.id).also { viewModel.setClipSpeed(0.5f + rng.nextFloat() * 3f) }
            4 -> viewModel.selectClip(clip.id).also { viewModel.updateClipTrim(rng.nextLong(600), 3000L) }
            5 -> viewModel.selectClip(clip.id).also {
                viewModel.updateClipAdjustments(rng.nextFloat() - 0.5f, 1.3f, 1.4f, 0.3f)
            }
            6 -> viewModel.selectClip(clip.id).also { viewModel.setClipVolume(1.5f) }
            7 -> viewModel.selectClip(clip.id).also { viewModel.setClipBlendMode(clip.id, BlendMode.entries.random(rng)) }
            8 -> viewModel.selectClip(clip.id).also { viewModel.setClipMask(clip.id, MaskData(type = MaskType.CIRCLE, isInverted = true)) }
            9 -> viewModel.selectClip(clip.id).also {
                viewModel.setClipHslAdjustments(
                    clip.id,
                    listOf(com.example.model.HslAdjustment(
                        channel = com.example.model.HslChannel.entries.random(rng),
                        hue = 10f, saturation = -20f, luminance = 3f
                    ))
                )
            }
            10 -> {
                viewModel.selectClip(clip.id)
                if (clip.keyframes.isNotEmpty()) {
                    viewModel.removeKeyframe(clip.id, clip.keyframes.first().id)
                } else {
                    viewModel.addKeyframe(
                        clip.id, KeyframeProperty.entries.random(rng), 1.2f, EasingCurve.entries.random(rng)
                    )
                }
            }
            11 -> viewModel.rotateSelectedClip()
            12 -> viewModel.mirrorSelectedClip()
            13 -> viewModel.moveClip(0, (project.clips.size - 1).coerceAtLeast(0))
            14 -> viewModel.addSticker("⭐")
            15 -> viewModel.addTimelineEffect(EffectType.entries.random(rng))
            16 -> {
                viewModel.addTextOverlay(
                    text = "fuzz", fontStyle = TextFontStyle.entries.random(rng),
                    color = 0xFFFFFFFF, animation = TextAnimationType.entries.random(rng)
                )
            }
            17 -> {
                viewModel.selectText(project.textOverlays.firstOrNull()?.id)
                viewModel.deleteSelectedText()
            }
            18 -> viewModel.setAspectRatio(AspectRatioType.entries.random(rng))
            19 -> viewModel.setPaperStyle("Blueprint")
        }
    }

    @Test
    fun `undo-all over random edit sequences restores the original project`() {
        repeat(25) { iteration ->
            val viewModel = seededViewModel(iteration.toLong() + 1)
            val rng = Random((iteration + 1) * 7919L) // deterministic, reproducible
            val original = viewModel.currentProject.value

            repeat(60) { applyRandomOp(viewModel, rng) }

            // Every mutating op must have been captured: the full drain must
            // reach the pre-edit snapshot exactly.
            var undos = 0
            while (viewModel.canUndo.value && undos < 500) {
                viewModel.undo()
                undos++
            }
            assertTrue("iteration=$iteration: history must drain fully", !viewModel.canUndo.value)
            assertTrue("iteration=$iteration: drained $undos entries, expected <= 60", undos <= 60)
            assertEquals("iteration=$iteration: undo-all must equal the original", original, viewModel.currentProject.value)
        }
    }

    @Test
    fun `redo-all after undo-all returns the latest state`() {
        val viewModel = seededViewModel(11L)
        val rng = Random(11L)
        repeat(30) { applyRandomOp(viewModel, rng) }
        val endState = viewModel.currentProject.value

        var undos = 0
        while (viewModel.canUndo.value && undos < 500) {
            viewModel.undo()
            undos++
        }
        var redos = 0
        while (viewModel.canRedo.value && redos < 500) {
            viewModel.redo()
            redos++
        }
        assertEquals(undos, redos)
        assertEquals(endState, viewModel.currentProject.value)
    }

    @Test
    fun `history cap holds in a long fuzz stream`() {
        val viewModel = seededViewModel(99L)
        val rng = Random(99L)
        repeat(220) { applyRandomOp(viewModel, rng) }

        var undos = 0
        while (viewModel.canUndo.value && undos < 500) {
            viewModel.undo()
            undos++
        }
        // Cap 200: the oldest 20 pushes were evicted, so the drain is <= 200
        // and the drained result is the earliest surviving snapshot, not the
        // original project.
        assertTrue(undos <= 200)
        assertFalse(viewModel.canUndo.value)
    }

    @Test
    fun `trim slider stream collapses into a single undo step`() {
        val viewModel = seededViewModel(7L)
        viewModel.selectClip("clip_1")
        val original = viewModel.currentProject.value

        // Rapid drag stream: three updates inside the coalescing window.
        viewModel.updateClipTrim(100L, 2500L)
        viewModel.updateClipTrim(200L, 2500L)
        viewModel.updateClipTrim(300L, 2500L)
        assertEquals(300L, viewModel.currentProject.value?.clips?.first()?.trimStartMs)

        // ONE undo reverts the entire gesture to the pre-gesture snapshot.
        viewModel.undo()
        assertEquals(original, viewModel.currentProject.value)
        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)
    }

    @Test
    fun `different targets do not coalesce`() {
        val viewModel = seededViewModel(8L)
        viewModel.selectClip("clip_1")
        viewModel.updateClipTrim(100L, 2500L) // gesture on clip_1
        viewModel.selectClip("clip_2")
        viewModel.updateClipTrim(50L, 2000L)  // gesture on clip_2

        // Two separate gestures -> two undo steps.
        viewModel.undo()
        val firstRestore = viewModel.currentProject.value
        // Clip 2 is restored untouched by the first undo (pre-gesture state).
        assertEquals(2500L, firstRestore?.clips?.find { it.id == "clip_2" }?.durationMs)
        // Clip 1 still carries its first gesture's trim (2400ms remaining).
        assertEquals(2400L, firstRestore?.clips?.find { it.id == "clip_1" }?.durationMs)

        viewModel.undo()
        val fullyRestored = viewModel.currentProject.value
        assertEquals(0L, fullyRestored?.clips?.find { it.id == "clip_1" }?.trimStartMs)
        assertEquals(0L, fullyRestored?.clips?.find { it.id == "clip_2" }?.trimStartMs)
        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)
    }
}
