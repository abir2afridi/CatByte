package com.example.data

import android.graphics.Matrix
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.OverlaySettings
import androidx.media3.common.SpeedParameters
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.AlphaScale
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.data.export.ColorGradeEffect
import com.example.data.export.ExportResolution
import com.example.data.export.FilterMapper
import com.example.engine.ClipAnimationMath
import com.example.data.export.HslColorEffect
import com.example.data.export.TimelineCanvasOverlay
import com.example.data.export.TransitionMapper
import com.example.engine.AudioSpec
import com.example.engine.LayerSpec
import com.example.engine.RenderGraph
import com.example.engine.RenderGraphBuilder
import com.example.model.AudioClip
import com.example.model.BlendMode
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.VideoProject
import kotlin.math.abs
import kotlin.math.max

/**
 * Builds one Media3 [Composition] for both the live preview and the exported
 * file. Preview and export therefore cannot drift apart: both consume exactly
 * the same sequences, trim points, speed providers, keyframes, text overlays and
 * audio processors.
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
    fun buildPreview(project: VideoProject, watermarkEnabled: Boolean): Result {
        val height = ExportResolution.heightFor(project.resolution).coerceIn(360, 720)
        val (width, safeHeight) = ExportResolution.outputSizeForHeight(project.aspectRatio, height)
        return build(project, width, safeHeight, project.fps.coerceIn(15, 60), watermarkEnabled)
    }

    fun buildExport(
        project: VideoProject,
        resolution: String,
        fps: Int,
        watermarkEnabled: Boolean
    ): Result {
        val (width, height) = ExportResolution.outputSize(project.aspectRatio, resolution)
        return build(project, width, height, fps.coerceIn(15, 60), watermarkEnabled)
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
        watermarkEnabled: Boolean
    ): Result {
        require(width > 0 && height > 0) { "Invalid output size ${width}x$height" }

        val renderPlan = plan(project, width, height, fps)
        val warnings = linkedSetOf<String>().apply { addAll(renderPlan.warnings) }
        val sequences = mutableListOf<EditedMediaItemSequence>()
        val overlayInputs = linkedMapOf<Int, MediaClip>()

        // 1. Primary video sequence. Placeholder clips become real timeline gaps
        // so the main track always occupies the same absolute time as the editor.
        val mainBuilder = EditedMediaItemSequence.Builder(
            setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO)
        )
        var cursorUs = 0L
        renderPlan.main.forEach { planned ->
            val gapUs = planned.layer.timelineInUs - cursorUs
            if (gapUs > 0L) {
                mainBuilder.addGap(gapUs)
            }
            if (planned.clip.uriString.isNullOrBlank()) {
                if (planned.layer.durationUs > 0L) {
                    mainBuilder.addGap(planned.layer.durationUs)
                }
            } else {
                mainBuilder.addItem(buildVideoItem(project, planned, fps, warnings))
            }
            cursorUs = planned.layer.timelineOutUs
        }
        val totalDurationUs = renderPlan.totalDurationUs.coerceAtLeast(1_000_000L)
        if (cursorUs < totalDurationUs) {
            // Pad the primary sequence so overlays/audio near the end are not cut
            // off by a shorter primary track.
            mainBuilder.addGap(totalDurationUs - cursorUs)
        }
        sequences += mainBuilder.build()

        // 2. Every PIP / overlay clip gets its own sequence. Sequence index is
        // exactly the video compositor input index; the graph supplies its start.
        renderPlan.overlays.forEach { planned ->
            if (planned.clip.uriString.isNullOrBlank()) {
                return@forEach
            }
            val builder = EditedMediaItemSequence.Builder(
                setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO)
            )
            if (planned.layer.timelineInUs > 0L) {
                builder.addGap(planned.layer.timelineInUs)
            }
            builder.addItem(buildVideoItem(project, planned, fps, warnings))
            val inputIndex = sequences.size
            sequences += builder.build()
            overlayInputs[inputIndex] = planned.clip
        }

        // 3. Independent audio tracks are mixed by CompositionPlayer during
        // preview and by Transformer during export.
        renderPlan.audio.forEach { (audio, spec) ->
            sequences += buildAudioSequence(audio, spec.timelineInUs)
        }

        val compositionEffects = mutableListOf<Effect>()
        compositionEffects += Presentation.createForWidthAndHeight(
            width,
            height,
            Presentation.LAYOUT_SCALE_TO_FIT
        )
        // A single full-frame canvas keeps all 2D layers in one texture and is
        // also where timeline effects and the watermark are burned in.
        compositionEffects += OverlayEffect(listOf(TimelineCanvasOverlay(project, watermarkEnabled)))

        val compositorSettings = ProjectCompositorSettings(width, height, overlayInputs)
        val compositionBuilder = Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), compositionEffects))
        // Media3 only builds a VideoCompositor when MORE THAN ONE sequence
        // carries a video track. With a single video track it uses
        // SingleInputVideoGraph, which rejects any custom VideoCompositorSettings
        // ("SingleInputVideoGraph does not use VideoCompositor...") and the
        // export dies before it starts. The custom settings only describe the
        // overlay (PIP) inputs, so they are only needed when overlays exist.
        if (overlayInputs.isNotEmpty()) {
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

    // ------------------------------------------------------------------
    // Video items
    // ------------------------------------------------------------------

    // Video items
    // ------------------------------------------------------------------

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

    private fun buildVideoItem(
        project: VideoProject,
        planned: PlannedClip,
        fps: Int,
        warnings: MutableSet<String>
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

        val videoEffects = mutableListOf<Effect>()
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
            .setEffects(Effects(audioProcessors, videoEffects))

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

    private fun buildAudioSequence(audio: AudioClip, startTimelineUs: Long): EditedMediaItemSequence {
        val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
        if (audio.trimStartMs > 0L || audio.trimEndMs > audio.trimStartMs) {
            clippingBuilder.setStartPositionMs(audio.trimStartMs.coerceAtLeast(0L))
            if (audio.trimEndMs > audio.trimStartMs) {
                clippingBuilder.setEndPositionMs(audio.trimEndMs)
            }
        }
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(audio.uriString))
            .setClippingConfiguration(clippingBuilder.build())
            .build()
        val gain = if (audio.isMuted) 0f else audio.volume.coerceIn(0f, 2f)
        val processor = GainProcessor(
            ConstantGainProvider(
                gain = gain,
                fadeInMs = audio.fadeInMs,
                fadeOutMs = audio.fadeOutMs,
                durationMs = audio.durationMs
            )
        )
        val itemBuilder = EditedMediaItem.Builder(mediaItem)
            .setDurationUs(audio.durationMs.coerceAtLeast(1L) * 1000L)
            .setRemoveVideo(true)
            .setEffects(Effects(listOf(processor), emptyList()))
        if (abs(audio.speed - 1f) > 0.001f && audio.speed > 0.01f) {
            itemBuilder.setSpeed(
                SpeedParameters(ConstantSpeedProvider(audio.speed.coerceIn(0.1f, 16f)), true)
            )
        }
        val item = itemBuilder.build()

        val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
        if (startTimelineUs > 0L) {
            builder.addGap(startTimelineUs)
        }
        builder.addItem(item)
        return builder.build()
    }

    // ------------------------------------------------------------------
    // Effects
    // ------------------------------------------------------------------

    private fun buildColorEffect(clip: MediaClip): ColorGradeEffect {
        val grade = FilterMapper.gradeFor(clip.filter)
        val contrast = (clip.contrast - 1f).coerceIn(-1f, 1f)
        val saturation = (clip.saturation * grade.saturation).coerceIn(0f, 2f)
        val hsl = HslColorEffect(clip.hslAdjustments)
        return ColorGradeEffect(
            brightness = (clip.brightness + grade.brightness + hsl.brightnessOffset()).coerceIn(-1f, 1f),
            contrast = (contrast + grade.contrast).coerceIn(-1f, 1f),
            saturation = (saturation * hsl.saturationMultiplier()).coerceIn(0f, 2f),
            redGain = grade.redGain * hsl.redMultiplier(),
            greenGain = grade.greenGain * hsl.greenMultiplier(),
            blueGain = grade.blueGain * hsl.blueMultiplier()
        )
    }
}

/**
 * Dynamic compositor settings: input 0 is the main video, inputs 1..N are PIP
 * clips. The overlay position, scale, rotation and alpha are evaluated for every
 * frame, so keyframed PIP motion is rendered identically in preview and export.
 */
@UnstableApi
private class ProjectCompositorSettings(
    private val width: Int,
    private val height: Int,
    private val overlayInputs: Map<Int, MediaClip>
) : VideoCompositorSettings {

    private val defaultSettings: OverlaySettings = StaticOverlaySettings.Builder()
        .setAlphaScale(1f)
        .build()

    override fun getOutputSize(inputSizes: List<Size>): Size = Size(width, height)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        val clip = overlayInputs[inputId] ?: return defaultSettings
        val timeMs = presentationTimeUs / 1000L
        val localMs = timeMs - clip.startTimelineMs
        if (localMs < 0L || localMs > clip.durationMs) {
            return StaticOverlaySettings.Builder().setAlphaScale(0f).build()
        }

        val posX = KeyframeInterpolator.getPosX(clip, localMs).coerceIn(0f, 1f)
        val posY = KeyframeInterpolator.getPosY(clip, localMs).coerceIn(0f, 1f)
        val scale = KeyframeInterpolator.getScale(clip, localMs).coerceAtLeast(0.02f)
        val rotation = KeyframeInterpolator.getRotation(clip, localMs)
        val opacity = KeyframeInterpolator.getOpacity(clip, localMs).coerceIn(0f, 1f)

        return StaticOverlaySettings.Builder()
            .setBackgroundFrameAnchor(
                (posX * 2f - 1f).coerceIn(-1f, 1f),
                (1f - posY * 2f).coerceIn(-1f, 1f)
            )
            .setOverlayFrameAnchor(0f, 0f)
            .setScale(scale, scale)
            // Android/Compose rotation is clockwise; OverlaySettings is CCW.
            .setRotationDegrees(-rotation)
            .setAlphaScale(opacity)
            .build()
    }
}

/** Main-track transform with per-frame keyframe support. */
@UnstableApi
private class ClipTransformEffect(private val clip: MediaClip) : MatrixTransformation {

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val timeMs = presentationTimeUs / 1000L
        val animation = ClipAnimationMath.transformAt(clip.animationIn, timeMs)
        val scale = KeyframeInterpolator.getScale(clip, timeMs).coerceAtLeast(0.01f) * animation.scale
        val rotation = KeyframeInterpolator.getRotation(clip, timeMs) + animation.rotationDeg
        val posX = KeyframeInterpolator.getPosX(clip, timeMs).coerceIn(0f, 1f)
        val posY = KeyframeInterpolator.getPosY(clip, timeMs).coerceIn(0f, 1f)

        val matrix = Matrix()
        val scaleX = if (clip.isMirrored) -scale else scale
        // MatrixTransformation operates in NDC; the origin is the frame centre.
        matrix.setScale(scaleX, scale, 0f, 0f)
        if (abs(rotation) > 0.001f) {
            matrix.postRotate(rotation, 0f, 0f)
        }
        val translateX = (posX - 0.5f) * 2f + animation.translateX
        val translateY = (0.5f - posY) * 2f + animation.translateY
        if (abs(translateX) > 0.0001f || abs(translateY) > 0.0001f) {
            matrix.postTranslate(translateX, translateY)
        }
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        clip.keyframes.isEmpty() &&
            clip.animationIn == com.example.model.ClipAnimation.NONE &&
            abs(clip.scale - 1f) < 0.0001f &&
            abs(clip.rotation) < 0.0001f &&
            !clip.isMirrored &&
            abs(clip.posX - 0.5f) < 0.0001f &&
            abs(clip.posY - 0.5f) < 0.0001f
}

/** Mirror/rotation for PIP textures; positioning is handled by the compositor. */
@UnstableApi
private class MirrorAndRotationEffect(private val clip: MediaClip) : MatrixTransformation {

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val matrix = Matrix()
        val scaleX = if (clip.isMirrored) -1f else 1f
        matrix.setScale(scaleX, 1f, 0f, 0f)
        if (abs(clip.rotation) > 0.001f) {
            matrix.postRotate(clip.rotation, 0f, 0f)
        }
        return matrix
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        !clip.isMirrored && abs(clip.rotation) < 0.001f
}
