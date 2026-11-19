package com.example.viewmodel.features

import com.example.model.BlendMode
import com.example.model.HslAdjustment
import com.example.model.MaskData
import com.example.viewmodel.EditorViewModel

/** Clip-level blend mode, mask and HSL adjustments. */
internal object ClipEffectFeature {

    fun setClipBlendMode(vm: EditorViewModel, clipId: String, mode: BlendMode) {
        vm.updateProject { proj ->
            val updatedClips = proj.clips.map { if (it.id == clipId) it.copy(blendMode = mode) else it }
            val updatedOverlayTracks = proj.overlayTracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(blendMode = mode) else it })
            }
            proj.copy(clips = updatedClips, overlayTracks = updatedOverlayTracks)
        }
    }

    fun setClipMask(vm: EditorViewModel, clipId: String, maskData: MaskData) {
        vm.updateProject { proj ->
            val updatedClips = proj.clips.map { if (it.id == clipId) it.copy(mask = maskData) else it }
            val updatedOverlayTracks = proj.overlayTracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(mask = maskData) else it })
            }
            proj.copy(clips = updatedClips, overlayTracks = updatedOverlayTracks)
        }
    }

    /**
     * Writes the per-channel HSL grade of one clip.
     *
     * The values are canonicalised (finite, inside the supported range) and
     * neutral channels are dropped, so the stored grade always describes a
     * real change. Slider drags share one coalescing key and therefore become
     * a single undo step.
     */
    fun setClipHslAdjustments(vm: EditorViewModel, clipId: String, adjustments: List<HslAdjustment>) {
        val canonical = adjustments
            .map { it.canonical() }
            .filterNot { it.isNeutral }
        vm.updateProject(coalesceKey = "hsl:$clipId") { proj ->
            val updatedClips = proj.clips.map { if (it.id == clipId) it.copy(hslAdjustments = canonical) else it }
            val updatedOverlayTracks = proj.overlayTracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(hslAdjustments = canonical) else it })
            }
            proj.copy(clips = updatedClips, overlayTracks = updatedOverlayTracks)
        }
    }
}
