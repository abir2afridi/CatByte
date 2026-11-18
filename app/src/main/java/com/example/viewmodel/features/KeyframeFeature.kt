package com.example.viewmodel.features

import com.example.engine.KenBurns
import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.localTimeForClip
import com.example.viewmodel.withUpdatedClip
import java.util.Locale

/** Clip keyframes: add at the playhead, remove, and toggle. */
internal object KeyframeFeature {

    fun addKeyframe(
        vm: EditorViewModel,
        clipId: String,
        property: KeyframeProperty,
        value: Float,
        easing: EasingCurve = EasingCurve.EASE_IN_OUT
    ) {
        val currentPos = vm._currentPositionMs.value
        vm.updateProject(coalesceKey = "keyframe:$clipId:$property") { proj ->
            val clipLocalMs = proj.localTimeForClip(clipId, currentPos) ?: return@updateProject proj
            proj.withUpdatedClip(clipId) { clip ->
                val existing = clip.keyframes.filterNot {
                    it.property == property &&
                        kotlin.math.abs(it.timestampMs - clipLocalMs) < 100L
                }
                val newKf = Keyframe(
                    timestampMs = clipLocalMs,
                    property = property,
                    value = value,
                    easing = easing
                )
                clip.copy(keyframes = (existing + newKf).sortedBy { it.timestampMs })
            }
        }
    }

    fun removeKeyframe(vm: EditorViewModel, clipId: String, keyframeId: String) {
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(keyframes = clip.keyframes.filterNot { it.id == keyframeId })
            }
        }
    }

    /** Toggles a keyframe at the current playhead position on the selected clip. */
    fun toggleKeyframeAtPlayhead(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        val currentPos = vm._currentPositionMs.value
        val project = vm._currentProject.value ?: return
        val clipLocalMs = project.localTimeForClip(clipId, currentPos) ?: return

        var existingKfId: String? = null
        project.clips.find { it.id == clipId }?.let { clip ->
            existingKfId = clip.keyframes.firstOrNull { kotlin.math.abs(it.timestampMs - clipLocalMs) < 150L }?.id
        }
        if (existingKfId == null) {
            project.overlayTracks.flatMap { it.clips }.find { it.id == clipId }?.let { clip ->
                existingKfId = clip.keyframes.firstOrNull { kotlin.math.abs(it.timestampMs - clipLocalMs) < 150L }?.id
            }
        }

        if (existingKfId != null) {
            removeKeyframe(vm, clipId, existingKfId!!)
        } else {
            addKeyframe(vm, clipId, KeyframeProperty.SCALE, 1.0f)
        }
    }

    /**
     * Toggles the slow "Ken Burns" push-in on the selected clip.
     *
     * The move is two ordinary SCALE keyframes, so preview and export
     * evaluate the same motion. The status string explains every outcome, so
     * the UI never shows a silent no-op.
     */
    fun toggleKenBurns(vm: EditorViewModel): String {
        val clipId = vm._selectedClipId.value ?: return "Select a clip first"
        val project = vm._currentProject.value ?: return "Select a clip first"
        val clip = project.clips.firstOrNull { it.id == clipId }
            ?: project.overlayTracks
                .asSequence()
                .flatMap { it.clips.asSequence() }
                .firstOrNull { it.id == clipId }
            ?: return "Select a clip first"

        if (clip.durationMs < KenBurns.MIN_DURATION_MS) {
            return "This clip is too short for a slow zoom"
        }

        if (KenBurns.isApplied(clip.keyframes, clip.durationMs, clip.scale)) {
            vm.updateProject { proj ->
                proj.withUpdatedClip(clipId) { updated ->
                    updated.copy(
                        keyframes = updated.keyframes.filterNot {
                            it.property == KeyframeProperty.SCALE
                        }
                    )
                }
            }
            return "Slow zoom removed"
        }

        if (KenBurns.hasConflictingKeyframes(clip.keyframes)) {
            return "Remove the existing scale/position keyframes first"
        }

        val endScale = KenBurns.endScale(clip.scale)
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { updated ->
                updated.copy(
                    keyframes = updated.keyframes + KenBurns.keyframes(
                        durationMs = updated.durationMs,
                        startScale = updated.scale
                    )
                )
            }
        }
        return "Slow zoom added (${formatScale(clip.scale)} to ${formatScale(endScale)})"
    }

    private fun formatScale(scale: Float): String =
        String.format(Locale.US, "%.2fx", scale)
}
