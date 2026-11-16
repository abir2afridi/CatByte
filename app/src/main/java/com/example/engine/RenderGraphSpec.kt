package com.example.engine

import com.example.model.BlendMode
import com.example.model.MaskType
import com.example.model.TextAnimationType

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
    /** Full source duration before clipping/speed (Media3 duration contract). */
    val sourceDurationUs: Long,
    /** Absolute source in/out points in microseconds. */
    val sourceInUs: Long,
    val sourceOutUs: Long,
    /** Source-time to output-time multiplier. */
    val speed: Float,
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
