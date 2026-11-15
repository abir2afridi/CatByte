package com.example.viewmodel.features

import com.example.model.HslAdjustment
import com.example.model.VideoFilter
import com.example.viewmodel.EditorViewModel

/** Snapshot of the visual/audio attributes that "paste attributes" copies. */
internal data class ClipAttributeClipboard(
    val filter: VideoFilter,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val vignette: Float,
    val hslAdjustments: List<HslAdjustment>,
    val speed: Float,
    val volume: Float,
    val isMuted: Boolean
)

/** Copy/paste of a clip's look and audio attributes onto other clips. */
internal object ClipAttributeFeature {

    /** Copies the selected clip's look/audio attributes for paste-attributes. */
    fun copySelectedClipAttributes(vm: EditorViewModel) {
        val project = vm._currentProject.value ?: return
        val clip = project.allVideoClips.firstOrNull { it.id == vm._selectedClipId.value } ?: return
        vm.attributeClipboard = ClipAttributeClipboard(
            filter = clip.filter,
            brightness = clip.brightness,
            contrast = clip.contrast,
            saturation = clip.saturation,
            vignette = clip.vignette,
            hslAdjustments = clip.hslAdjustments,
            speed = clip.speed,
            volume = clip.volume,
            isMuted = clip.isMuted
        )
    }

    /** Applies the copied look/audio attributes to every selected clip. */
    fun pasteClipAttributes(vm: EditorViewModel) {
        val clipboard = vm.attributeClipboard ?: return
        val ids = vm._selectedClipIds.value
        if (ids.isEmpty()) return
        vm.updateProject { proj ->
            proj.copy(
                clips = proj.clips.map { clip ->
                    if (clip.id in ids) {
                        clip.copy(
                            filter = clipboard.filter,
                            brightness = clipboard.brightness,
                            contrast = clipboard.contrast,
                            saturation = clipboard.saturation,
                            vignette = clipboard.vignette,
                            hslAdjustments = clipboard.hslAdjustments,
                            speed = clipboard.speed,
                            volume = clipboard.volume,
                            isMuted = clipboard.isMuted
                        )
                    } else {
                        clip
                    }
                },
                overlayTracks = proj.overlayTracks.map { track ->
                    track.copy(
                        clips = track.clips.map { clip ->
                            if (clip.id in ids) {
                                clip.copy(
                                    filter = clipboard.filter,
                                    brightness = clipboard.brightness,
                                    contrast = clipboard.contrast,
                                    saturation = clipboard.saturation,
                                    vignette = clipboard.vignette,
                                    hslAdjustments = clipboard.hslAdjustments,
                                    speed = clipboard.speed,
                                    volume = clipboard.volume,
                                    isMuted = clipboard.isMuted
                                )
                            } else {
                                clip
                            }
                        }
                    )
                }
            )
        }
    }
}
