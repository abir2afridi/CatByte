package com.example.viewmodel.features

import com.example.model.EffectType
import com.example.model.TimelineEffect
import com.example.viewmodel.EditorViewModel
import java.util.UUID

/** Timeline effect overlays (RGB split, shake, VHS grain, etc.). */
internal object TimelineEffectFeature {

    fun addTimelineEffect(vm: EditorViewModel, effectType: EffectType) {
        val effect = TimelineEffect(
            id = UUID.randomUUID().toString(),
            name = effectType.displayName,
            effectType = effectType,
            startTimelineMs = vm._currentPositionMs.value,
            durationMs = 2500L
        )
        vm.updateProject { proj ->
            proj.copy(effects = proj.effects + effect)
        }
    }

    fun deleteTimelineEffect(vm: EditorViewModel, id: String) {
        vm.updateProject { proj ->
            proj.copy(effects = proj.effects.filterNot { it.id == id })
        }
    }

    /**
     * Moves an effect's window on the timeline (the drag gesture on the
     * effects track). Overlapping another effect is legal - both render; the
     * track packs overlapping chips into separate lanes.
     */
    fun moveTimelineEffect(vm: EditorViewModel, id: String, startTimelineMs: Long) {
        vm.updateProject { proj ->
            proj.copy(
                effects = proj.effects.map { fx ->
                    if (fx.id == id) {
                        fx.copy(startTimelineMs = startTimelineMs.coerceAtLeast(0L))
                    } else {
                        fx
                    }
                }
            )
        }
    }

    /**
     * The effect that covers the playhead, or null when there is none.
     *
     * The effect panel's "None" card removes exactly this one, so the card
     * can be disabled when the playhead is not on an effect.
     */
    fun effectAtPlayhead(vm: EditorViewModel): TimelineEffect? {
        val position = vm._currentPositionMs.value
        return vm.currentProject.value?.effects?.firstOrNull { effect ->
            position >= effect.startTimelineMs &&
                position < effect.startTimelineMs + effect.durationMs
        }
    }

    /** Removes the effect that covers the playhead, if there is one. */
    fun removeEffectAtPlayhead(vm: EditorViewModel) {
        val effect = effectAtPlayhead(vm) ?: return
        deleteTimelineEffect(vm, effect.id)
    }
}
