package com.example.engine

import com.example.model.AudioClip
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.TextOverlay
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject

/**
 * Builds the single render graph from the editing model.
 *
 * Main-track clips are laid out sequentially (that is how the editor sums their
 * durations), overlay clips keep their absolute start, and transitions are
 * clamped to half of the shorter neighbour so a cut can never eat a whole clip.
 */
object RenderGraphBuilder {

    fun build(project: VideoProject): RenderGraph {
        val fps = Rational.fromLegacy(project.fps)
        val (width, height) = CanvasMath.canvasSize(project.aspectRatio, project.resolution)
        val warnings = mutableListOf<String>()
        val layers = mutableListOf<LayerSpec>()

        var cursorUs = 0L
        project.clips.forEachIndexed { index, clip ->
            val inUs = cursorUs
            val outUs = inUs + clip.durationMs.coerceAtLeast(0L) * 1_000L
            layers += layerOf(clip, zOrder = 0, inUs = inUs, outUs = outUs, fps = fps)
            cursorUs = outUs

            if (clip.transition != TransitionType.NONE && index == 0) {
                warnings += "Transition on the first clip has nothing to blend into"
            }
        }

        project.overlayTracks.forEachIndexed { trackIndex, track ->
            track.clips.forEach { clip ->
                val inUs = clip.startTimelineMs.coerceAtLeast(0L) * 1_000L
                val outUs = inUs + clip.durationMs.coerceAtLeast(0L) * 1_000L
                layers += layerOf(clip, zOrder = trackIndex + 1, inUs = inUs, outUs = outUs, fps = fps)
            }
        }

        // Clamp transitions once all the neighbours are known.
        val clamped = layers.map { layer -> layer.copy(transitionIn = clampedTransition(layer, layers)) }

        val audio = project.audioTracks.map { it.toSpec() }
        val texts = project.textOverlays.map { it.toSpec() }
        val globalEffects = project.effects.map {
            EffectSpec(
                id = it.id,
                params = mapOf("effect" to it.effectType.ordinal.toFloat())
            )
        }

        val measuredUs = project.totalDurationMs * 1_000L
        val durationUs = TimeUs(measuredUs).snapToFrame(fps, FrameRounding.UP).value

        return RenderGraph(
            projectId = project.id,
            canvasWidth = width,
            canvasHeight = height,
            fps = fps,
            durationUs = durationUs,
            layers = clamped.sortedWith(compareBy({ it.zOrder }, { it.timelineInUs })),
            audio = audio,
            texts = texts,
            globalEffects = globalEffects,
            backgroundArgb = backgroundArgb(project.paperStyle),
            warnings = warnings
        )
    }

    private fun clampedTransition(layer: LayerSpec, layers: List<LayerSpec>): TransitionSpec? {
        val requested = layer.transitionIn ?: return null
        val previous = layers
            .filter { it.zOrder == layer.zOrder && it.timelineOutUs <= layer.timelineInUs + 1_000L }
            .maxByOrNull { it.timelineOutUs } ?: return null
        return requested.copy(
            durationUs = TransitionMath.clampDurationUs(
                requestedUs = requested.durationUs,
                previousDurationUs = previous.durationUs,
                currentDurationUs = layer.durationUs
            )
        )
    }

    private fun layerOf(
        clip: MediaClip,
        zOrder: Int,
        inUs: Long,
        outUs: Long,
        fps: Rational
    ): LayerSpec {
        val sourceInUs = TimeUs.fromMs(clip.trimStartMs.coerceAtLeast(0L)).value
        val sourceOutUs = if (clip.trimEndMs > clip.trimStartMs) {
            TimeUs.fromMs(clip.trimEndMs).value
        } else {
            TimeUs.fromMs(clip.originalDurationMs.coerceAtLeast(clip.trimStartMs)).value
        }
        val transition = if (clip.transition != TransitionType.NONE) {
            TransitionSpec(
                kind = TransitionMath.fromLegacy(clip.transition),
                durationUs = clip.transitionDurationMs.coerceAtLeast(0L) * 1_000L
            )
        } else {
            null
        }
        return LayerSpec(
            clipId = clip.id,
            assetKey = clip.uriString ?: clip.drawableResId?.toString(),
            zOrder = zOrder,
            timelineInUs = inUs,
            timelineOutUs = outUs,
            sourceInUs = sourceInUs,
            sourceOutUs = sourceOutUs,
            speed = clip.speed.coerceIn(0.05f, 32f),
            volume = clip.volume.coerceIn(0f, 1f),
            opacity = clip.opacity.coerceIn(0f, 1f),
            blend = clip.blendMode,
            transform = TransformSpec(
                scale = clip.scale.coerceIn(0.01f, 16f),
                posX = clip.posX,
                posY = clip.posY,
                rotationDeg = clip.rotation,
                mirrored = clip.isMirrored
            ),
            mask = if (clip.mask.type == MaskType.NONE) null else GraphMaskSpec(
                kind = clip.mask.type,
                posX = clip.mask.posX,
                posY = clip.mask.posY,
                width = clip.mask.width,
                height = clip.mask.height,
                rotationDeg = clip.mask.rotation,
                feather = clip.mask.feather.coerceIn(0f, 1f),
                inverted = clip.mask.isInverted
            ),
            effects = effectsOf(clip),
            transitionIn = transition
        )
    }

    /** Clip grade + legacy colour adjustments, in stable shader order. */
    private fun effectsOf(clip: MediaClip): List<EffectSpec> {
        val effects = mutableListOf<EffectSpec>()
        if (clip.filter != VideoFilter.NONE) {
            effects += EffectSpec(
                id = "filter_${clip.filter.name.lowercase()}",
                params = mapOf("preset" to clip.filter.ordinal.toFloat())
            )
        }
        if (clip.brightness != 0f || clip.contrast != 1f || clip.saturation != 1f || clip.vignette != 0f) {
            effects += EffectSpec(
                id = "color_grade",
                params = mapOf(
                    "brightness" to clip.brightness,
                    "contrast" to clip.contrast,
                    "saturation" to clip.saturation,
                    "vignette" to clip.vignette
                )
            )
        }
        clip.hslAdjustments.forEach { adjustment ->
            effects += EffectSpec(
                id = "hsl_${adjustment.channel.name.lowercase()}",
                params = mapOf(
                    "hue" to adjustment.hue,
                    "saturation" to adjustment.saturation,
                    "luminance" to adjustment.luminance
                )
            )
        }
        return effects
    }

    private fun AudioClip.toSpec(): AudioSpec {
        val inUs = startTimelineMs.coerceAtLeast(0L) * 1_000L
        val presentationDurationMs = AudioTiming.presentationDurationMs(this)
        val lengthUs = presentationDurationMs.coerceAtLeast(AudioTiming.MIN_AUDIO_DURATION_MS) * 1_000L
        val sourceDurationUs = AudioTiming.sourceDurationMs(this).coerceAtLeast(1L) * 1_000L
        val sourceInUs = AudioTiming.sourceStartMs(this).coerceAtLeast(0L) * 1_000L
        val sourceOutUs = AudioTiming.sourceEndMs(this)
            .coerceIn(AudioTiming.sourceStartMs(this), AudioTiming.sourceDurationMs(this)) * 1_000L
        val fades = AudioTiming.clampFades(
            durationMs = presentationDurationMs,
            requestedFadeInMs = fadeInMs,
            requestedFadeOutMs = fadeOutMs
        )
        return AudioSpec(
            clipId = id,
            assetKey = uriString,
            timelineInUs = inUs,
            timelineOutUs = inUs + lengthUs,
            sourceDurationUs = sourceDurationUs,
            sourceInUs = sourceInUs,
            sourceOutUs = sourceOutUs,
            speed = AudioTiming.normalizedSpeed(speed),
            volume = if (isMuted) 0f else volume.coerceIn(0f, 2f),
            fadeInUs = fades.first * 1_000L,
            fadeOutUs = fades.second * 1_000L,
            loopCount = AudioTiming.loopCount(this)
        )
    }

    private fun TextOverlay.toSpec(): TextSpec {
        val inUs = startTimelineMs.coerceAtLeast(0L) * 1_000L
        return TextSpec(
            overlayId = id,
            text = text,
            timelineInUs = inUs,
            timelineOutUs = inUs + durationMs.coerceAtLeast(0L) * 1_000L,
            posX = posX,
            posY = posY,
            scale = scale,
            colorArgb = color,
            animation = animation
        )
    }

    private fun backgroundArgb(paperStyle: String): Long = backgroundArgbFor(paperStyle)
}
