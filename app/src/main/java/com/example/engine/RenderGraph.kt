package com.example.engine

import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.BlendMode
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.TextAnimationType
import com.example.model.TextOverlay
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.model.VideoProject
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** One clip as the compositor sees it: no editor state, only render data. */
data class LayerSpec(
    val clipId: String,
    val assetKey: String?,
    val zOrder: Int,
    val timelineInUs: Long,
    val timelineOutUs: Long,
    val sourceInUs: Long,
    val sourceOutUs: Long,
    val speed: Float,
    val volume: Float,
    val opacity: Float,
    val blend: BlendMode,
    val transform: TransformSpec,
    val mask: GraphMaskSpec?,
    val effects: List<EffectSpec>,
    val transitionIn: TransitionSpec?
) {
    val durationUs: Long get() = (timelineOutUs - timelineInUs).coerceAtLeast(0L)
}

data class TransformSpec(
    val scale: Float = 1f,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val rotationDeg: Float = 0f,
    val mirrored: Boolean = false
)

typealias RenderGraphMask = com.example.engine.GraphMaskSpec

data class GraphMaskSpec(
    val kind: MaskType,
    val posX: Float,
    val posY: Float,
    val width: Float,
    val height: Float,
    val rotationDeg: Float,
    val feather: Float,
    val inverted: Boolean
)

data class EffectSpec(
    val id: String,
    val params: Map<String, Float> = emptyMap()
)

data class AudioSpec(
    val clipId: String,
    val assetKey: String?,
    val timelineInUs: Long,
    val timelineOutUs: Long,
    val volume: Float,
    val fadeInUs: Long,
    val fadeOutUs: Long
)

data class TextSpec(
    val overlayId: String,
    val text: String,
    val timelineInUs: Long,
    val timelineOutUs: Long,
    val posX: Float,
    val posY: Float,
    val scale: Float,
    val colorArgb: Long,
    val animation: TextAnimationType
)

/**
 * Immutable description of every frame and audio sample of a project.
 *
 * Preview and export both consume this, so a feature can never work in one and
 * silently degrade in the other.
 */
data class RenderGraph(
    val projectId: String,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val fps: Rational,
    val durationUs: Long,
    val layers: List<LayerSpec>,
    val audio: List<AudioSpec>,
    val texts: List<TextSpec>,
    val globalEffects: List<EffectSpec>,
    val backgroundArgb: Long = 0xFF000000,
    val warnings: List<String> = emptyList()
)

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
        val lengthUs = durationMs.coerceAtLeast(0L) * 1_000L
        return AudioSpec(
            clipId = id,
            assetKey = uriString,
            timelineInUs = inUs,
            timelineOutUs = inUs + lengthUs,
            volume = if (isMuted) 0f else volume.coerceIn(0f, 2f),
            fadeInUs = fadeInMs.coerceIn(0L, durationMs).coerceAtLeast(0L) * 1_000L,
            fadeOutUs = fadeOutMs.coerceIn(0L, durationMs).coerceAtLeast(0L) * 1_000L
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

    private fun backgroundArgb(paperStyle: String): Long = when (paperStyle.lowercase()) {
        "blueprint" -> 0xFF0B1E33
        "sepia" -> 0xFF2A2118
        "charcoal" -> 0xFF141414
        else -> 0xFF000000
    }
}

/** Convenience: sample the graph at a timeline position. */
fun RenderGraph.layersAt(positionUs: Long): List<LayerSpec> =
    layers.filter { positionUs >= it.timelineInUs && positionUs < it.timelineOutUs }

fun RenderGraph.textsAt(positionUs: Long): List<TextSpec> =
    texts.filter { positionUs >= it.timelineInUs && positionUs < it.timelineOutUs }

/** Frame index of a presentation time, used by the golden-frame harness. */
fun RenderGraph.frameAt(positionUs: Long): Long = TimeUs(positionUs).toFrame(fps)

/** Rounds a duration up to whole frames (export needs an exact frame count). */
fun RenderGraph.totalFrames(): Long = TimeUs(durationUs).toFrame(fps).coerceAtLeast(1L)

/** Milliseconds of a frame count, for UI labels. */
fun RenderGraph.frameCountToMs(frames: Long): Long =
    (TimeUs.fromFrames(frames, fps).value / 1_000.0).roundToLong()
