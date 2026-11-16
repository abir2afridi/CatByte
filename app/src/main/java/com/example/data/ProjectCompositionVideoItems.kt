package com.example.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.GainProcessor
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.effect.AlphaScale
import com.example.data.composition.ClipTransformEffect
import com.example.data.composition.MirrorAndRotationEffect
import com.example.data.export.ConstantGainProvider
import com.example.data.export.TransitionMapper
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlin.math.abs
import kotlin.math.max

/**
 * Audio gain of a video clip, honouring clip mute, track mute and video
 * track solo. Music clips (separate `AudioClip`s) own their own mute flag;
 * this only covers the audio that comes out of picture tracks.
 */
private fun videoVoiceGain(project: VideoProject, clip: MediaClip, isOverlay: Boolean): Float {
    if (clip.isMuted) return 0f
    val anyVideoSolo = project.isMainTrackSolo || project.overlayTracks.any { it.isSolo }
    val gain = if (isOverlay) {
        val track = project.overlayTracks.firstOrNull { it.clips.any { c -> c.id == clip.id } }
        val muted = track?.isMuted ?: false
        val solo = track?.isSolo ?: false
        if (muted || (anyVideoSolo && !solo)) 0f else clip.volume
    } else {
        if (project.isMainTrackMuted || (anyVideoSolo && !project.isMainTrackSolo)) {
            0f
        } else {
            clip.volume
        }
    }
    return gain.coerceIn(0f, 10f)
}

/** Builds one [EditedMediaItem] for a planned main-track or overlay clip. */
internal fun ProjectCompositionFactory.buildVideoItem(
    project: VideoProject,
    planned: ProjectCompositionFactory.PlannedClip,
    fps: Int,
    warnings: MutableSet<String>,
    removeVideo: Boolean = false
): EditedMediaItem {
    val clip = planned.clip
    val layer = planned.layer
    val isOverlay = planned.isOverlay
    val uri = clip.uriString ?: error("Clip ${clip.title} has no media URI")
    val sourceLengthMs = ClipTiming.sourceLengthMs(clip)
    val originalDurationMs = max(clip.originalDurationMs, sourceLengthMs).coerceAtLeast(1L)

    val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
    if (clip.trimEndMs > clip.trimStartMs) {
        clippingBuilder.setEndPositionMs(clip.trimEndMs)
    }

    val mediaItem = MediaItem.Builder()
        .setUri(Uri.parse(uri))
        .setClippingConfiguration(clippingBuilder.build())
        .build()

    val videoEffects = mutableListOf<androidx.media3.common.Effect>()
    videoEffects += buildColorEffect(clip)
    if (isOverlay) {
        if (clip.rotation != 0f || clip.isMirrored) {
            videoEffects += MirrorAndRotationEffect(clip)
        }
    } else {
        videoEffects += ClipTransformEffect(clip)
        videoEffects += TransitionMapper.effectsFor(
            transition = clip.transition,
            clipDurationUs = layer.durationUs.coerceAtLeast(1L),
            // The graph already clamped the duration to half of the shorter
            // neighbour, so preview and export share one decision.
            transitionDurationMs = (layer.transitionIn?.durationUs ?: 0L) / 1000L
        )
        // CapCut's Opacity control blends the whole clip over the canvas.
        // Applied after the transition so the slider's value survives.
        if (clip.opacity < 0.999f) {
            videoEffects += AlphaScale(clip.opacity.coerceIn(0f, 1f))
        }
    }

    val audioProcessors = mutableListOf<AudioProcessor>()
    val gain = videoVoiceGain(project, clip, isOverlay)
    if (!isOverlay || gain != 1f) {
        if (abs(gain - 1f) > 0.001f) {
            audioProcessors += GainProcessor(
                ConstantGainProvider(gain, 0L, 0L, layer.durationUs / 1000L)
            )
        }
    }

    val builder = EditedMediaItem.Builder(mediaItem)
        .setDurationUs(originalDurationMs * 1000L)
        .setFrameRate(fps)
        // Audio-only export: keep the audio graph, drop every video track.
        .setRemoveVideo(removeVideo)
        .setEffects(Effects(audioProcessors, if (removeVideo) emptyList() else videoEffects))

    if (ClipTiming.hasSpeedCurve(clip) || abs(clip.speed - 1f) > 0.001f) {
        builder.setSpeed(
            SpeedParameters(
                ClipSpeedProvider(clip, sourceLengthMs * 1000L),
                /* shouldMaintainPitch= */ clip.maintainPitch
            )
        )
    }

    if (isOverlay && clip.durationMs <= 0L) {
        warnings += "Overlay '${clip.title}' has no duration."
    }
    return builder.build()
}
