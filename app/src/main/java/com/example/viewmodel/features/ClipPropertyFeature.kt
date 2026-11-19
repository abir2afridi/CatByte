package com.example.viewmodel.features

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.ClipTiming
import com.example.data.MediaMetadata
import com.example.engine.AudioLoudness
import com.example.model.AudioEffectSettings
import com.example.model.ClipAnimation
import com.example.model.SpeedCurve
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.withUpdatedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Trim, speed, colour, audio and source properties of the selected clip. */
internal object ClipPropertyFeature {

    /** Updates the trim of the selected clip. Streams collapse into one undo. */
    fun updateClipTrim(vm: EditorViewModel, trimStartMs: Long, trimEndMs: Long) {
        val clipId = vm._selectedClipId.value ?: return
        if (vm._currentProject.value?.isTrackLockedForClip(clipId) == true) return
        vm.updateProject(coalesceKey = "trim:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val newTrimStart = trimStartMs.coerceIn(
                    0L,
                    clip.originalDurationMs.coerceAtLeast(300L)
                )
                val newTrimEnd = trimEndMs.coerceIn(
                    newTrimStart + 300L,
                    clip.originalDurationMs.coerceAtLeast(newTrimStart + 300L)
                )
                val trimmed = clip.copy(
                    trimStartMs = newTrimStart,
                    trimEndMs = newTrimEnd
                )
                trimmed.copy(durationMs = ClipTiming.presentationDurationMs(trimmed))
            }
        }
    }

    /**
     * Sets a constant speed. Picking a constant speed clears the velocity curve
     * (the two modes are mutually exclusive, like CapCut), and the timeline
     * duration is recomputed through the shared [ClipTiming] implementation.
     */
    fun setClipSpeed(vm: EditorViewModel, speed: Float) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "speed:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val updatedClip = clip.copy(
                    speed = speed.coerceIn(0.1f, ClipTiming.MAX_SPEED),
                    speedCurve = SpeedCurve()
                )
                updatedClip.copy(durationMs = ClipTiming.presentationDurationMs(updatedClip))
            }
        }
    }

    /** Applies a velocity curve to the selected clip and retimes the timeline. */
    fun setClipSpeedCurve(vm: EditorViewModel, curve: SpeedCurve) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "speedCurve:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val updatedClip = clip.copy(speedCurve = curve)
                updatedClip.copy(durationMs = ClipTiming.presentationDurationMs(updatedClip))
            }
        }
    }

    fun setClipFilter(vm: EditorViewModel, filter: VideoFilter) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(filter = filter) }
        }
    }

    /**
     * Per-clip audio effects (voice preset, enhance voice, noise gate). The
     * same [AudioEffectSettings] reaches both preview and export through the
     * shared composition factory.
     */
    fun setClipAudioEffects(vm: EditorViewModel, settings: AudioEffectSettings) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(audioEffects = settings) }
        }
    }

    /**
     * Fade in/out of the selected clip's own audio, in output-timeline
     * milliseconds. Overlapping fades are scaled proportionally so the gain
     * envelope never multiplies twice - the same rule the audio tracks use.
     */
    fun setClipAudioFades(vm: EditorViewModel, fadeInMs: Long, fadeOutMs: Long) {
        val clipId = vm._selectedClipId.value ?: return
        val project = vm._currentProject.value ?: return
        val clip = project.allVideoClips.firstOrNull { it.id == clipId } ?: return
        val duration = ClipTiming.presentationDurationMs(clip).coerceAtLeast(1L)
        val fades = clampClipFades(duration, fadeInMs, fadeOutMs)
        if (clip.audioFadeInMs == fades.first && clip.audioFadeOutMs == fades.second) return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { current ->
                current.copy(audioFadeInMs = fades.first, audioFadeOutMs = fades.second)
            }
        }
    }

    /** Keeps both fades inside the clip and scales them when they overlap. */
    private fun clampClipFades(
        durationMs: Long,
        requestedFadeInMs: Long,
        requestedFadeOutMs: Long
    ): Pair<Long, Long> {
        val duration = durationMs.coerceAtLeast(1L)
        val fadeIn = requestedFadeInMs.coerceIn(0L, duration)
        val fadeOut = requestedFadeOutMs.coerceIn(0L, duration)
        if (fadeIn <= duration - fadeOut) return fadeIn to fadeOut
        val scale = duration.toDouble() / (fadeIn.toDouble() + fadeOut.toDouble())
        return (fadeIn * scale).toLong() to (fadeOut * scale).toLong()
    }

    fun setClipTransition(vm: EditorViewModel, transition: TransitionType) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(transition = transition) }
        }
    }

    fun setClipAnimation(vm: EditorViewModel, animation: ClipAnimation) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(animationIn = animation) }
        }
    }

    /** Keeps (default) or drops the source pitch when the clip is retimed. */
    fun setClipMaintainPitch(vm: EditorViewModel, maintain: Boolean) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(maintainPitch = maintain) }
        }
    }

    fun rotateSelectedClip(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(rotation = (clip.rotation + 90f) % 360f)
            }
        }
    }

    fun mirrorSelectedClip(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(isMirrored = !clip.isMirrored) }
        }
    }

    fun updateClipAdjustments(
        vm: EditorViewModel,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        vignette: Float
    ) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "adjust:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(
                    brightness = brightness.coerceIn(-1f, 1f),
                    contrast = contrast.coerceIn(0.1f, 2f),
                    saturation = saturation.coerceIn(0f, 2f),
                    vignette = vignette.coerceIn(0f, 1f)
                )
            }
        }
    }

    /** Volume slider streams collapse into one undo step. */
    fun setClipVolume(vm: EditorViewModel, volume: Float) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "volume:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip -> clip.copy(volume = volume.coerceIn(0f, 10f)) }
        }
    }

    /** Absolute opacity for the selected clip; keyframes still override it. */
    fun setClipOpacity(vm: EditorViewModel, opacity: Float) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "opacity:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(opacity = opacity.coerceIn(0f, 1f))
            }
        }
    }

    /**
     * Absolute crop/zoom transform. Crop presets compute these values with
     * [com.example.engine.CropMath]; storing the absolute scale (rather than a
     * multiplier) keeps repeated preset taps idempotent.
     */
    fun setSelectedClipCrop(vm: EditorViewModel, scale: Float, posX: Float, posY: Float) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "crop:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(
                    scale = scale.coerceIn(1f, 5f),
                    posX = posX.coerceIn(0f, 1f),
                    posY = posY.coerceIn(0f, 1f)
                )
            }
        }
    }

    /**
     * Absolute rotation of the selected clip, in degrees.
     *
     * The rotation is part of the clip's transform, so the preview and the
     * exported file both show the turned picture.
     */
    fun setSelectedClipRotation(vm: EditorViewModel, rotationDeg: Float) {
        val clipId = vm._selectedClipId.value ?: return
        val normalized = if (rotationDeg.isFinite()) {
            ((rotationDeg % 360f) + 360f) % 360f
        } else {
            0f
        }
        vm.updateProject(coalesceKey = "rotation:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(rotation = normalized)
            }
        }
    }

    /**
     * Mirrors the selected clip horizontally.
     *
     * The mirror flag is part of the clip's transform, so the preview and the
     * exported file both show the flipped picture.
     */
    fun setSelectedClipMirrored(vm: EditorViewModel, mirrored: Boolean) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "mirror:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                clip.copy(isMirrored = mirrored)
            }
        }
    }

    /**
     * Real loudness normalisation driven by the clip's decoded waveform. The
     * resulting gain is written to [com.example.model.MediaClip.volume], which
     * preview playback and the export GainProcessor both read, so the switch is
     * not cosmetic.
     */
    fun setClipLoudnessAdjustment(vm: EditorViewModel, enabled: Boolean) {
        val clipId = vm._selectedClipId.value ?: return
        vm.updateProject(coalesceKey = "loudness:$clipId") { proj ->
            proj.withUpdatedClip(clipId) { clip ->
                val canNormalise = AudioLoudness.canNormalise(clip.waveform)
                val gain = if (enabled && canNormalise) {
                    AudioLoudness.gainFor(clip.waveform)
                } else {
                    1f
                }
                clip.copy(
                    volume = gain.coerceIn(0f, 10f),
                    isLoudnessAdjusted = enabled && canNormalise
                )
            }
        }
    }

    /**
     * Probes media dimensions for projects imported before source-size
     * persistence existed, so Crop's aspect presets use the real frame.
     */
    fun ensureSelectedClipSourceSize(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        val clip = vm._currentProject.value?.allVideoClips?.find { it.id == clipId } ?: return
        if (clip.sourceWidth > 0 && clip.sourceHeight > 0) return
        val uriString = clip.uriString ?: return
        val ctx = vm.context ?: return
        vm.viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                runCatching { MediaMetadata.videoInfo(ctx, Uri.parse(uriString)) }.getOrNull()
            } ?: return@launch
            if (info.width <= 0 || info.height <= 0) return@launch
            vm.updateProject { proj ->
                proj.withUpdatedClip(clipId) {
                    it.copy(sourceWidth = info.width, sourceHeight = info.height)
                }
            }
        }
    }

    /**
     * Lazily decodes the selected clip's waveform. Imported video clips start
     * with an empty envelope for speed, so the Volume panel calls this before
     * offering loudness normalisation.
     */
    fun ensureSelectedClipWaveform(vm: EditorViewModel) {
        val clipId = vm._selectedClipId.value ?: return
        val clip = vm._currentProject.value?.allVideoClips?.find { it.id == clipId } ?: return
        if (clip.waveform.isNotEmpty()) return
        val uriString = clip.uriString ?: return
        val ctx = vm.context ?: return
        vm.viewModelScope.launch {
            val waveform = withContext(Dispatchers.IO) {
                runCatching { MediaMetadata.waveform(ctx, Uri.parse(uriString), 48) }.getOrNull()
            } ?: return@launch
            if (waveform.isEmpty()) return@launch
            vm.updateProject { proj ->
                proj.withUpdatedClip(clipId) { it.copy(waveform = waveform) }
            }
        }
    }

    /**
     * Replaces the selected clip's source file while keeping its position on
     * the timeline. Duration, dimensions and the waveform are re-probed so the
     * new media lines up in preview and export instead of keeping stale values.
     */
    fun replaceSelectedClipUri(vm: EditorViewModel, uriString: String, title: String) {
        val clipId = vm._selectedClipId.value ?: return
        val ctx = vm.context
        vm.viewModelScope.launch {
            val info = if (ctx != null) {
                runCatching { MediaMetadata.videoInfo(ctx, Uri.parse(uriString)) }.getOrNull()
            } else {
                null
            }
            val sourceMs = info?.durationMs?.takeIf { it > 0L } ?: 4_000L
            val realTitle = title.takeIf { it.isNotBlank() }
                ?: info?.displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                ?: "Replaced clip"
            vm.updateProject { proj ->
                proj.withUpdatedClip(clipId) { clip ->
                    clip.copy(
                        title = realTitle,
                        uriString = uriString,
                        originalDurationMs = sourceMs,
                        durationMs = sourceMs,
                        trimStartMs = 0L,
                        trimEndMs = 0L,
                        speed = 1f,
                        speedCurve = SpeedCurve(),
                        sourceWidth = info?.width ?: 0,
                        sourceHeight = info?.height ?: 0,
                        waveform = emptyList(),
                        isLoudnessAdjusted = false
                    )
                }
            }
        }
    }
}
