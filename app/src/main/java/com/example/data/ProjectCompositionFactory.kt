package com.example.data

import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.Effects
import com.example.data.composition.ProjectCompositorSettings
import com.example.data.export.ExportResolution
import com.example.data.export.TimelineCanvasOverlay
import com.example.engine.AudioSpec
import com.example.engine.LayerSpec
import com.example.engine.RenderGraph
import com.example.engine.RenderGraphBuilder
import com.example.model.AudioClip
import com.example.model.BlendMode
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.VideoProject

/**
 * Builds one Media3 [Composition] for both the live preview and the exported
 * file. Preview and export therefore cannot drift apart: both consume exactly
 * the same sequences, trim points, speed providers, keyframes, text overlays and
 * audio processors.
 *
 * Feature code lives next to this host as extension functions:
 * `ProjectCompositionSequences.kt`, `ProjectCompositionVideoItems.kt`,
 * `ProjectCompositionAudioSequence.kt`, `ProjectCompositionColorEffect.kt`.
 */
@UnstableApi
object ProjectCompositionFactory {

    data class Result(
        val composition: Composition,
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val overlayInputs: Map<Int, MediaClip>,
        val warnings: List<String>
    )

    /** One clip positioned by the render graph instead of by the editor model. */
    data class PlannedClip(
        val clip: MediaClip,
        val layer: LayerSpec,
        val isOverlay: Boolean
    )

    /**
     * The resolved render layout: graph timing decisions matched back to the
     * model objects Media3 needs. Pure data, so preview/export parity can be
     * asserted without an encoder or a device.
     */
    data class RenderPlan(
        val graph: RenderGraph,
        val canvasWidth: Int,
        val canvasHeight: Int,
        val fps: Int,
        val main: List<PlannedClip>,
        val overlays: List<PlannedClip>,
        val audio: List<Pair<AudioClip, AudioSpec>>,
        val warnings: List<String>
    ) {
        val totalDurationUs: Long get() = graph.durationUs
        val aspectRatio: Float get() = canvasWidth.toFloat() / canvasHeight.toFloat()
    }

    /**
     * Preview uses a smaller render target than a final export. Keeping the
     * aspect ratio identical means every coordinate and effect lands in the same
     * place when the project is exported.
     */
    fun buildPreview(
        project: VideoProject,
        watermarkEnabled: Boolean,
        /** Decoded canvas background image (null = colour background only). */
        backgroundImage: android.graphics.Bitmap? = null
    ): Result {
        val height = ExportResolution.heightFor(project.resolution).coerceIn(360, 720)
        val (width, safeHeight) = ExportResolution.outputSizeForHeight(project.aspectRatio, height)
        return build(
            project,
            width,
            safeHeight,
            project.fps.coerceIn(15, 60),
            watermarkEnabled,
            backgroundImage = backgroundImage
        )
    }

    fun buildExport(
        project: VideoProject,
        resolution: String,
        fps: Int,
        watermarkEnabled: Boolean,
        removeVideo: Boolean = false,
        backgroundImage: android.graphics.Bitmap? = null
    ): Result {
        val (width, height) = ExportResolution.outputSize(project.aspectRatio, resolution)
        return build(
            project,
            width,
            height,
            fps.coerceIn(15, 60),
            watermarkEnabled,
            removeVideo,
            backgroundImage
        )
    }

    /**
     * Resolves the project into the exact sequence layout the compositor renders.
     *
     * Clip order, gaps, clamped transitions and audio start times all come from
     * [RenderGraphBuilder], i.e. the same graph the preview consumes, so a
     * transition can no longer be 800 ms in the preview and 400 ms in the file.
     */
    fun plan(project: VideoProject, width: Int, height: Int, fps: Int): RenderPlan {
        val graph = RenderGraphBuilder.build(project)
        val clipsById = project.allVideoClips.associateBy { it.id }
        val audioById = project.audioTracks.associateBy { it.id }
        val warnings = linkedSetOf<String>()
        val main = mutableListOf<PlannedClip>()
        val overlays = mutableListOf<PlannedClip>()

        graph.layers.forEach { layer ->
            val clip = clipsById[layer.clipId] ?: return@forEach
            val isOverlay = layer.zOrder > 0
            if (clip.uriString.isNullOrBlank()) {
                if (isOverlay) {
                    warnings += "Overlay '${clip.title}' has no media and was skipped."
                } else {
                    warnings += "Placeholder clip '${clip.title}' is rendered as a gap."
                }
            }
            if (clip.mask.type != MaskType.NONE) {
                warnings += "Mask on '${clip.title}' is not burned into the render yet."
            }
            if (clip.blendMode != BlendMode.NORMAL) {
                warnings += "Blend mode '${clip.blendMode.displayName}' on '${clip.title}' " +
                    "is composited with normal alpha blending."
            }
            val planned = PlannedClip(clip = clip, layer = layer, isOverlay = isOverlay)
            if (isOverlay) overlays += planned else main += planned
        }

        val audio = graph.audio.mapNotNull { spec ->
            val clip = audioById[spec.clipId] ?: return@mapNotNull null
            if (clip.uriString.isNullOrBlank()) {
                warnings += "Audio '${clip.title}' has no media and was skipped."
                null
            } else {
                clip to spec
            }
        }

        warnings += graph.warnings
        return RenderPlan(
            graph = graph,
            canvasWidth = width,
            canvasHeight = height,
            fps = fps,
            main = main,
            overlays = overlays,
            audio = audio,
            warnings = warnings.toList()
        )
    }

    fun build(
        project: VideoProject,
        width: Int,
        height: Int,
        fps: Int,
        watermarkEnabled: Boolean,
        /** Drops every video track so Transformer renders an audio-only file. */
        removeVideo: Boolean = false,
        /** Decoded canvas background image (null = colour background only). */
        backgroundImage: android.graphics.Bitmap? = null
    ): Result {
        require(width > 0 && height > 0) { "Invalid output size ${width}x$height" }

        val renderPlan = plan(project, width, height, fps)
        val warnings = linkedSetOf<String>().apply { addAll(renderPlan.warnings) }
        val (sequences, overlayInputs) =
            buildSequences(project, renderPlan, fps, warnings, removeVideo)

        val compositionEffects = mutableListOf<Effect>()
        if (!removeVideo) {
            compositionEffects += Presentation.createForWidthAndHeight(
                width,
                height,
                Presentation.LAYOUT_SCALE_TO_FIT
            )
        }
        // A single full-frame canvas keeps all 2D layers in one texture and is
        // also where timeline effects and the watermark are burned in.
        if (!removeVideo) {
            compositionEffects += OverlayEffect(
                listOf(TimelineCanvasOverlay(project, watermarkEnabled, backgroundImage))
            )
        }

        val compositorSettings = ProjectCompositorSettings(width, height, overlayInputs)
        val compositionBuilder = Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), compositionEffects))
        // Media3 only builds a VideoCompositor when MORE THAN ONE sequence
        // carries a video track. With a single video track it uses
        // SingleInputVideoGraph, which rejects any custom VideoCompositorSettings
        // ("SingleInputVideoGraph does not use VideoCompositor...") and the
        // export dies before it starts. The custom settings only describe the
        // overlay (PIP) inputs, so they are only needed when overlays exist.
        if (!removeVideo && overlayInputs.isNotEmpty()) {
            compositionBuilder.setVideoCompositorSettings(compositorSettings)
        }
        val composition = compositionBuilder.build()

        return Result(
            composition = composition,
            width = width,
            height = height,
            durationMs = renderPlan.totalDurationUs / 1000L,
            overlayInputs = overlayInputs,
            warnings = warnings.toList()
        )
    }
}
