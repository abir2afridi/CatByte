package com.example.viewmodel

import com.example.data.ProjectRepository
import com.example.model.EffectType
import com.example.model.MediaClip
import com.example.model.TimelineEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Moving a placed effect on the timeline (the effects-track drag): the window
 * shifts, negative starts clamp to 0, and overlap with another effect stays
 * legal - both effects survive.
 */
class MoveTimelineEffectTest {

    private fun viewModelWith(vararg effects: TimelineEffect): EditorViewModel {
        val viewModel = EditorViewModel(repository = ProjectRepository())
        val project = ProjectRepository().createNewProject(
            "Effects",
            initialClips = listOf(
                MediaClip(id = "clip_1", title = "Clip 1", uriString = "content://c/1", durationMs = 8_000)
            )
        )
        viewModel.openProject(project)
        viewModel.updateProject { proj ->
            proj.copy(effects = effects.toList())
        }
        return viewModel
    }

    private fun effect(id: String, startMs: Long, durationMs: Long = 2_000) = TimelineEffect(
        id = id,
        name = id,
        effectType = EffectType.RGB_SPLIT,
        startTimelineMs = startMs,
        durationMs = durationMs
    )

    @Test
    fun `moving shifts the window and keeps duration`() {
        val viewModel = viewModelWith(effect("fx1", 1_000))
        viewModel.moveTimelineEffect("fx1", 4_000)
        val fx = viewModel.currentProject.value!!.effects.single()
        assertEquals(4_000L, fx.startTimelineMs)
        assertEquals(2_000L, fx.durationMs)
    }

    @Test
    fun `negative start clamps to zero`() {
        val viewModel = viewModelWith(effect("fx1", 1_000))
        viewModel.moveTimelineEffect("fx1", -5_000)
        assertEquals(0L, viewModel.currentProject.value!!.effects.single().startTimelineMs)
    }

    @Test
    fun `overlapping another effect is legal - both survive`() {
        val viewModel = viewModelWith(effect("fx1", 0), effect("fx2", 3_000))
        viewModel.moveTimelineEffect("fx2", 500) // now overlaps fx1
        val effects = viewModel.currentProject.value!!.effects
        assertEquals(2, effects.size)
        assertTrue(effects.any { it.id == "fx1" && it.startTimelineMs == 0L })
        assertTrue(effects.any { it.id == "fx2" && it.startTimelineMs == 500L })
    }

    @Test
    fun `moving is undoable`() {
        val viewModel = viewModelWith(effect("fx1", 1_000))
        viewModel.moveTimelineEffect("fx1", 5_000)
        viewModel.undo()
        assertEquals(1_000L, viewModel.currentProject.value!!.effects.single().startTimelineMs)
    }
}
