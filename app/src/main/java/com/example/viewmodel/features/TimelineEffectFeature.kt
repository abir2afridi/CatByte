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
}
