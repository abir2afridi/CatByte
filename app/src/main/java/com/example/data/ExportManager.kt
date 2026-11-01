package com.example.data

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
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
import com.example.data.export.CodecCapabilityChecker
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

        // Resolve codec + output size against real device capabilities.
        // Fallback ladder: HEVC -> H.264 for codecs, 4K -> 1080P -> 720P -> 480P
        // for frame sizes. Nothing here fails the export - if nothing matches we
        // fall back to Media3's default encoder (graceful degradation).
        _progress.value = ExportProgress(progress = 0f, stage = "Checking device capabilities...")
        val target = CodecCapabilityChecker.resolve(project.aspectRatio, resolution, fps)
        val downgradeNote = when {
            target.downgraded && target.fpsDowngraded ->
                "Device limit: exporting at ${target.resolutionLabel}, ${target.fps} fps"

            target.downgraded -> "Device limit: exporting at ${target.resolutionLabel}"
            target.fpsDowngraded -> "Device limit: exporting at ${target.fps} fps"
            else -> null
        }
        if (downgradeNote != null) {
            AppLog.warn("ExportManager", downgradeNote)
        } else {
            AppLog.info(
                "ExportManager",
                "Export target: ${target.width}x${target.height}@${target.fps}fps (${target.mime ?: "default encoder"})"
            )
        }
        if (target.mime == null) {
            AppLog.warn(
                "ExportManager",
                "No encoder matched the target capabilities; Media3 will choose a default encoder"
            )
        }

        // Storage precheck: estimate = bitrate x duration x 1.1, using the
        // resolved output size (never larger than what the user asked for).
        val estimatedSize = estimateExportSize(target.width, target.height, project.totalDurationMs)
        val availableSpace = context.getExternalFilesDir(null)?.usableSpace ?: 0L
        if (availableSpace < estimatedSize) {
            _progress.value = ExportProgress(
                error = "Insufficient storage: need ~${estimatedSize / 1024 / 1024}MB, have ${availableSpace / 1024 / 1024}MB available"
            )
            AppLog.warn("ExportManager", "Export aborted: insufficient storage")
            return@withContext null
        }

        val outWidth = target.width
        val outHeight = target.height
        _progress.value = ExportProgress(
            progress = 0f,
            stage = downgradeNote ?: "Preparing timeline..."
        )

        val outputDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val outputFile = File(outputDir, "CatByte_${project.id}_${System.currentTimeMillis()}.mp4")

        val composition = try {
            buildComposition(project, mediaClips, fps, outWidth, outHeight, watermarkEnabled)
        } catch (error: Throwable) {
            AppLog.error("ExportManager", "Failed to build composition, falling back to plain export", error)
            buildPlainComposition(mediaClips)
        }

        var run = runTransformer(composition, outputFile, target.mime)
        // If a pinned codec was rejected by the encoder, retry once with the
        // default codec path instead of failing the whole export.
        if (run.result == null && target.mime != null && isEncoderRelatedFailure(run.failure)) {
            AppLog.warn("ExportManager", "Codec ${target.mime} was rejected; retrying with the default encoder")
            _progress.value = _progress.value.copy(progress = 0f, stage = "Retrying with the default encoder...")
            run = runTransformer(composition, outputFile, null)
        }
        val exportResult = run.result
        if (exportResult == null) {
            _progress.value = _progress.value.copy(
                error = run.failure?.message ?: "Export failed",
                isCancelled = false
            )
            return@withContext null
        }

        val galleryUri = runCatching {
            MediaStoreSaver.saveVideo(context, outputFile, "CatByte_${project.title.take(24).replace("/", "-")}")
        }.getOrNull()

        // Validate exported MP4 before reporting success
        val validation = validateExport(outputFile, project)
        if (!validation.isValid) {
            _progress.value = ExportProgress(
                progress = 0f,
                stage = "Export validation failed",
                error = validation.error ?: "Export validation failed",
                isCancelled = false
            )
            AppLog.error("ExportManager", "Export validation failed: ${validation.error}")
            return@withContext null
        }

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

    /**
     * Validates an exported MP4 file using MediaMetadataRetriever.
     * Checks: file exists, has video track, duration within tolerance.
     */
    fun validateExport(outputFile: File, project: VideoProject): ValidationResult {
        val file = outputFile
        if (!file.exists()) {
            return ValidationResult(false, "Exported file does not exist: ${outputFile.absolutePath}")
        }
        val fileSize = file.length()
        if (fileSize == 0L) {
            return ValidationResult(false, "Exported file is empty: ${outputFile.absolutePath}")
        }
        AppLog.info("ExportManager", "Validating export: ${outputFile.name} (${fileSize} bytes)")

        return runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(outputFile.absolutePath)

            // Check video track dimensions
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (widthStr == null || heightStr == null) {
                return ValidationResult(false, "Exported video has no video track dimensions")
            }
            val width = widthStr.toIntOrNull() ?: 0
            val height = heightStr.toIntOrNull() ?: 0
            if (width <= 0 || height <= 0) {
                return ValidationResult(false, "Invalid video dimensions: ${width}x${height}")
            }

            // Check duration
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val expectedDurationMs = project.totalDurationMs
            val durationDiff = kotlin.math.abs(durationMs - expectedDurationMs)
            if (durationMs == 0L) {
                return ValidationResult(false, "Exported video has zero duration")
            }
            if (durationDiff > 1000L) { // Allow 1 second tolerance
                return ValidationResult(false, "Duration mismatch: expected ${expectedDurationMs}ms, got ${durationMs}ms (diff ${durationDiff}ms)")
            }

            AppLog.info("ExportManager", "Export validation PASSED: ${outputFile.name} (${width}x${height}, ${durationMs}ms)")
            ValidationResult(true, null)
        }.getOrElse { e ->
            AppLog.warn("ExportManager", "Export validation failed for $outputFile", e)
            ValidationResult(false, "Validation error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val error: String?
    )

    /**
     * Estimates export file size: bitrate × duration × 1.1 safety margin.
     * Bitrate is chosen from the real output size (post capability downgrade).
     */
    private fun estimateExportSize(width: Int, height: Int, durationMs: Long): Long {
        val bitrateMbps = when {
            width >= 3840 -> 45.0   // 4K
            width >= 1920 -> 16.0   // 1080p
            width >= 1280 -> 8.0    // 720p
            else -> 5.0             // 480p
        }
        val bitrateBps = bitrateMbps * 1_000_000L
        val durationSeconds = durationMs / 1000.0
        val estimatedBytes = (bitrateBps * durationSeconds * 1.1).toLong()
        return estimatedBytes
    }

    /**
     * Query MediaCodecList for encoders supporting the given resolution/fps.
     * Kept as a thin wrapper over [CodecCapabilityChecker] so existing callers
     * and tests keep working. Returns ordered mime types (HEVC before AVC).
     */
    fun querySupportedCodecs(width: Int, height: Int, fps: Int): List<String> =
        CodecCapabilityChecker.querySupportedCodecs(width, height, fps).map { it.mime }

    /** True when the failure looks like the device encoder refusing our codec. */
    private fun isEncoderRelatedFailure(failure: Throwable?): Boolean {
        if (failure == null) return true
        if (failure !is ExportException) return false
        return failure.errorCode == ExportException.ERROR_CODE_ENCODER_INIT_FAILED ||
            failure.errorCode == ExportException.ERROR_CODE_ENCODING_FAILED ||
            failure.errorCode == ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED
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

    /** Outcome of a single Transformer run. */
    private data class TransformerRun(
        val result: ExportResult? = null,
        val failure: Throwable? = null
    )

    /**
     * Runs the render. When [videoMime] is set the Transformer is asked for that
     * output codec (e.g. HEVC); `null` leaves Media3's default encoder choice.
     */
    private suspend fun runTransformer(
        composition: Composition,
        outputFile: File,
        videoMime: String? = null
    ): TransformerRun = coroutineScope {
        var outcome = TransformerRun()

        val newTransformer = Transformer.Builder(context)
            .apply { videoMime?.let { mime -> setVideoMimeType(mime) } }
            .build()
        transformer = newTransformer

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

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    outcome = TransformerRun(result = exportResult)
                    poller.cancel()
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    outcome = TransformerRun(failure = exportException)
                    poller.cancel()
                    AppLog.error("ExportManager", "Video export failed", exportException)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
            newTransformer.addListener(listener)

            runCatching { newTransformer.start(composition, outputFile.absolutePath) }
                .onFailure { startError ->
                    outcome = TransformerRun(failure = startError)
                    poller.cancel()
                    AppLog.error("ExportManager", "Could not start the render", startError)
                    if (continuation.isActive) continuation.resume(Unit)
                }
        }

        transformer = null
        outcome
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
