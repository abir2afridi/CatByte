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

    fun setClipHslAdjustments(vm: EditorViewModel, clipId: String, adjustments: List<HslAdjustment>) {
        vm.updateProject { proj ->
            val updatedClips = proj.clips.map { if (it.id == clipId) it.copy(hslAdjustments = adjustments) else it }
            val updatedOverlayTracks = proj.overlayTracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == clipId) it.copy(hslAdjustments = adjustments) else it })
            }
            proj.copy(clips = updatedClips, overlayTracks = updatedOverlayTracks)
        }
    }
}
