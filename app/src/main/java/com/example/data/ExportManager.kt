package com.example.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.export.ColorGradeEffect
import com.example.data.export.ExportPresets
import com.example.data.export.FilterMapper
import com.example.data.export.OverlayRenderer
import com.example.data.export.TimedBitmapOverlay
import com.example.data.export.TransitionMapper
import com.example.model.AudioClip
import com.example.model.MediaClip
import com.example.model.TextFontStyle
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Renders a CatByte project into a real MP4 file using the Media3 Transformer.
 *
 * Everything the user sees in the preview is applied to the exported file:
 * filters, colour adjustments, rotation / mirroring, transitions, speed,
 * per-clip volume, text and sticker overlays and the watermark.
 */
@UnstableApi
class ExportManager(private val context: Context) {

    data class ExportProgress(
        val progress: Float = 0f,
        val stage: String = "",
        val isDone: Boolean = false,
        val outputPath: String? = null,
        val galleryUri: Uri? = null,
        val error: String? = null,
        val isCancelled: Boolean = false
    )

    data class ExportOutput(
        val file: File,
        val galleryUri: Uri?,
        val durationMs: Long,
        val width: Int,
        val height: Int
    )

    private val _progress = MutableStateFlow(ExportProgress())
    val progress: StateFlow<ExportProgress> = _progress.asStateFlow()

    @Volatile
    private var transformer: Transformer? = null

    /**
     * Exports [project] to an MP4 file. Suspends until completion or failure and
     * returns the rendered file, or null when the export failed.
     */
    suspend fun export(
        project: VideoProject,
        resolution: String,
        fps: Int,
        watermarkEnabled: Boolean = false
    ): ExportOutput? = withContext(Dispatchers.Main) {
        val mediaClips = project.clips.filter { !it.uriString.isNullOrBlank() }
        if (mediaClips.isEmpty()) {
            _progress.value = ExportProgress(error = "No video clips with media to export.")
            AppLog.error("ExportManager", "Export aborted: project has no media backed clips")
            return@withContext null
        }

        val (outWidth, outHeight) = ExportPresets.outputSize(project.aspectRatio, resolution)
        _progress.value = ExportProgress(progress = 0f, stage = "Preparing timeline...")

        val outputDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val outputFile = File(outputDir, "CatByte_${project.id}_${System.currentTimeMillis()}.mp4")

        val composition = try {
            buildComposition(project, mediaClips, fps, outWidth, outHeight, watermarkEnabled)
        } catch (error: Throwable) {
            AppLog.error("ExportManager", "Failed to build composition, falling back to plain export", error)
            buildPlainComposition(mediaClips)
        }

        val exportResult = runTransformer(composition, outputFile)
        if (exportResult == null) {
            return@withContext null
        }

        val galleryUri = runCatching {
            MediaStoreSaver.saveVideo(context, outputFile, "CatByte_${project.title.take(24).replace("/", "-")}")
        }.getOrNull()

        _progress.value = ExportProgress(
            progress = 1f,
            stage = "Export complete!",
            isDone = true,
            outputPath = outputFile.absolutePath,
            galleryUri = galleryUri
        )
        AppLog.info(
            "ExportManager",
            "Export finished: ${outputFile.name} (${exportResult.width}x${exportResult.height}, " +
                "${exportResult.durationMs}ms, ${exportResult.fileSizeBytes / 1024}KB)"
        )

        ExportOutput(
            file = outputFile,
            galleryUri = galleryUri,
            durationMs = exportResult.durationMs,
            width = exportResult.width,
            height = exportResult.height
        )
    }

    /** Cancels an in-flight render. */
    fun cancel() {
        transformer?.cancel()
        transformer = null
        AppLog.warn("ExportManager", "Export cancelled by user")
        _progress.value = _progress.value.copy(error = "Export cancelled", isCancelled = true, isDone = false)
    }

    // ---------------------------------------------------------------------
    // Composition building
    // ---------------------------------------------------------------------

    private fun buildComposition(
        project: VideoProject,
        mediaClips: List<MediaClip>,
        fps: Int,
        outWidth: Int,
        outHeight: Int,
        watermarkEnabled: Boolean
    ): Composition {
        val videoItems = mutableListOf<EditedMediaItem>()
        var timelineUs = 0L
        mediaClips.forEach { clip ->
            val durationUs = effectiveDurationUs(clip)
            val item = buildVideoItem(clip, fps, timelineUs, timelineUs + durationUs)
            videoItems += item
            timelineUs += durationUs
        }

        val sequences = mutableListOf<EditedMediaItemSequence>()
        sequences += EditedMediaItemSequence.Builder(videoItems).build()

        project.audioTracks.filter { !it.uriString.isNullOrBlank() }.forEach { audio ->
            sequences += buildAudioSequence(audio)
        }

        val compositionEffects =
            buildCompositionEffects(project, outWidth, outHeight, watermarkEnabled, timelineUs)

        return Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), compositionEffects))
            .build()
    }

    /** Fallback composition without any effects - used when effect creation fails. */
    private fun buildPlainComposition(mediaClips: List<MediaClip>): Composition {
        val items = mediaClips.map { clip ->
            EditedMediaItem.Builder(MediaItem.fromUri(clip.uriString!!.toUri())).build()
        }
        return Composition.Builder(EditedMediaItemSequence.Builder(items).build()).build()
    }

    private fun effectiveDurationUs(clip: MediaClip): Long {
        val sourceLength = sourceLengthMs(clip)
        val speed = clip.speed.coerceAtLeast(0.1f)
        return (sourceLength / speed * 1000L).toLong().coerceAtLeast(1000L)
    }

    private fun sourceLengthMs(clip: MediaClip): Long {
        val start = clip.trimStartMs.coerceAtLeast(0L)
        val end = if (clip.trimEndMs > clip.trimStartMs) clip.trimEndMs else clip.originalDurationMs
        val length = (end - start).coerceAtLeast(300L)
        return length.coerceAtMost(clip.originalDurationMs.coerceAtLeast(length))
    }

    private fun buildVideoItem(
        clip: MediaClip,
        fps: Int,
        startUs: Long,
        endUs: Long
    ): EditedMediaItem {
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
            .apply {
                if (clip.trimEndMs > clip.trimStartMs) {
                    setEndPositionMs(clip.trimEndMs)
                }
            }
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(clip.uriString!!.toUri())
            .setClippingConfiguration(clipping)
            .build()

        val videoEffects = mutableListOf<Effect>()
        videoEffects += buildColorEffect(clip)
        videoEffects += buildTransformEffect(clip)
        videoEffects += TransitionMapper.effectsFor(
            transition = clip.transition,
            clipStartUs = startUs,
            clipEndUs = endUs,
            transitionDurationMs = clip.transitionDurationMs
        )

        val audioProcessors = mutableListOf<AudioProcessor>()
        if (clip.volume > 0f && clip.volume < 1f) {
            audioProcessors += GainProcessor(ConstantGainProvider(clip.volume, 0L, 0L, 0L))
        }

        val builder = EditedMediaItem.Builder(mediaItem)
            .setFrameRate(fps.coerceIn(15, 60))
            .setEffects(Effects(audioProcessors, videoEffects))

        if (abs(clip.speed - 1f) > 0.001f && clip.speed > 0.01f) {
            builder.setSpeed(SpeedParameters(ConstantSpeedProvider(clip.speed), true))
        }
        return builder.build()
    }

    private fun buildColorEffect(clip: MediaClip): ColorGradeEffect {
        val grade = FilterMapper.gradeFor(clip.filter)
        val vignetteScale = 1f - clip.vignette.coerceIn(0f, 1f) * 0.28f
        val contrast = (clip.contrast - 1f).coerceIn(-1f, 1f)
        val saturation = (clip.saturation * grade.saturation).coerceIn(0f, 2f)
        return ColorGradeEffect(
            brightness = clip.brightness + grade.brightness,
            contrast = contrast + grade.contrast,
            saturation = saturation,
            redGain = grade.redGain * vignetteScale,
            greenGain = grade.greenGain * vignetteScale,
            blueGain = grade.blueGain * vignetteScale
        )
    }

    /** Rotation / mirroring / scaling of a clip. */
    private fun buildTransformEffect(clip: MediaClip): MatrixTransformation =
        object : MatrixTransformation {
            override fun getMatrix(presentationTimeUs: Long): android.graphics.Matrix {
                val matrix = android.graphics.Matrix()
                val scaleX = if (clip.isMirrored) -clip.scale else clip.scale
                matrix.setScale(scaleX, clip.scale, 0.5f, 0.5f)
                if (clip.rotation != 0f) {
                    matrix.postRotate(clip.rotation, 0.5f, 0.5f)
                }
                return matrix
            }

            override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
                clip.rotation == 0f && !clip.isMirrored && clip.scale == 1f
        }

    private fun buildAudioSequence(audio: AudioClip): EditedMediaItemSequence {
        val mediaItem = MediaItem.fromUri(audio.uriString!!.toUri())
        val gain = if (audio.isMuted) 0f else audio.volume.coerceIn(0f, 2f)
        val processor = GainProcessor(
            ConstantGainProvider(
                gain = gain,
                fadeInMs = audio.fadeInMs,
                fadeOutMs = audio.fadeOutMs,
                durationMs = audio.durationMs
            )
        )
        val item = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .setEffects(Effects(listOf(processor), emptyList()))
            .build()

        val builder = EditedMediaItemSequence.Builder(emptyList<EditedMediaItem>())
        if (audio.startTimelineMs > 0) {
            builder.addGap(audio.startTimelineMs * 1000L)
        }
        builder.addItem(item)
        return builder.build()
    }

    private fun buildCompositionEffects(
        project: VideoProject,
        outWidth: Int,
        outHeight: Int,
        watermarkEnabled: Boolean,
        timelineUs: Long
    ): List<Effect> {
        val effects = mutableListOf<Effect>()

        effects += Presentation.createForWidthAndHeight(
            outWidth,
            outHeight,
            Presentation.LAYOUT_SCALE_TO_FIT
        )

        val overlays = mutableListOf<TextureOverlay>()
        project.textOverlays.forEach { text ->
            val bitmap = OverlayRenderer.renderTextLayer(
                frameWidth = outWidth,
                frameHeight = outHeight,
                text = text.text,
                color = text.color,
                fontStyle = text.fontStyle,
                posX = text.posX,
                posY = text.posY,
                scale = text.scale,
                withShadow = text.fontStyle == TextFontStyle.NEON_GLOW
            ) ?: return@forEach
            overlays += TimedBitmapOverlay(
                bitmap = bitmap,
                startUs = text.startTimelineMs * 1000L,
                endUs = (text.startTimelineMs + text.durationMs) * 1000L,
                overlaySettings = StaticOverlaySettings.Builder().setAlphaScale(1f).build()
            )
        }

        project.stickers.forEach { sticker ->
            val bitmap = OverlayRenderer.renderStickerLayer(
                frameWidth = outWidth,
                frameHeight = outHeight,
                symbol = sticker.symbol,
                posX = sticker.posX,
                posY = sticker.posY,
                scale = sticker.scale
            ) ?: return@forEach
            overlays += TimedBitmapOverlay(
                bitmap = bitmap,
                startUs = sticker.startTimelineMs * 1000L,
                endUs = (sticker.startTimelineMs + sticker.durationMs) * 1000L,
                overlaySettings = StaticOverlaySettings.Builder().setAlphaScale(1f).build()
            )
        }

        if (watermarkEnabled) {
            OverlayRenderer.renderWatermarkLayer(outWidth, outHeight)?.let { bitmap ->
                overlays += TimedBitmapOverlay(
                    bitmap = bitmap,
                    startUs = 0L,
                    endUs = timelineUs.coerceAtLeast(1L),
                    overlaySettings = StaticOverlaySettings.Builder().setAlphaScale(0.75f).build()
                )
            }
        }

        if (overlays.isNotEmpty()) {
            effects += OverlayEffect(overlays)
        }
        return effects
    }

    // ---------------------------------------------------------------------
    // Transformer
    // ---------------------------------------------------------------------

    private suspend fun runTransformer(composition: Composition, outputFile: File): ExportResult? =
        coroutineScope {
            var completed: ExportResult? = null
            var failure: ExportException? = null

            val newTransformer = Transformer.Builder(context).build()
            transformer = newTransformer

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    completed = exportResult
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    failure = exportException
                }
            }
            newTransformer.addListener(listener)

            suspendCancellableCoroutine<Unit> { continuation ->
                val holder = ProgressHolder()
                val poller = launch {
                    while (isActive) {
                        val state = runCatching { newTransformer.getProgress(holder) }.getOrDefault(0)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            val fraction = (holder.progress / 100f).coerceIn(0f, 1f)
                            _progress.value = _progress.value.copy(
                                progress = fraction,
                                stage = "Rendering video... ${holder.progress}%"
                            )
                        }
                        delay(180L)
                    }
                }

                continuation.invokeOnCancellation {
                    poller.cancel()
                    runCatching { newTransformer.cancel() }
                }

                val completionListener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        poller.cancel()
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        poller.cancel()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
                newTransformer.addListener(completionListener)

                runCatching { newTransformer.start(composition, outputFile.absolutePath) }
                    .onFailure {
                        poller.cancel()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
            }

            transformer = null

            val error = failure
            if (error != null) {
                AppLog.error("ExportManager", "Video export failed", error)
                _progress.value = _progress.value.copy(
                    error = error.message ?: "Export failed",
                    isCancelled = false
                )
                return@coroutineScope null
            }
            completed
        }
}

/** A constant playback speed for a clip. */
@UnstableApi
class ConstantSpeedProvider(private val speed: Float) : SpeedProvider {
    override fun getSpeed(presentationTimeUs: Long): Float = speed
    override fun getNextSpeedChangeTimeUs(presentationTimeUs: Long): Long = C.TIME_END_OF_SOURCE
}

/** Applies a constant volume with optional fade in / fade out. */
@UnstableApi
class ConstantGainProvider(
    private val gain: Float,
    private val fadeInMs: Long,
    private val fadeOutMs: Long,
    private val durationMs: Long
) : GainProcessor.GainProvider {

    override fun getGainFactorAtSamplePosition(presentationTimeUs: Long, sampleRate: Int): Float {
        val timeMs = presentationTimeUs / 1000L
        var factor = gain.coerceIn(0f, 2f)
        if (fadeInMs > 0 && timeMs < fadeInMs) {
            factor *= (timeMs.toFloat() / fadeInMs).coerceIn(0f, 1f)
        }
        if (fadeOutMs > 0 && durationMs > 0) {
            val remaining = durationMs - timeMs
            if (remaining < fadeOutMs) {
                factor *= (remaining.toFloat() / fadeOutMs).coerceIn(0f, 1f)
            }
        }
        return factor
    }

    override fun isUnityUntil(presentationTimeUs: Long, sampleRate: Int): Long = Long.MIN_VALUE
}
