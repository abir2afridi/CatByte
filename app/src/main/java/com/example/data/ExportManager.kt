package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.data.export.CodecCapabilityChecker
import com.example.engine.AudioMath
import com.example.engine.ExportPresets as EngineExportPresets
import com.example.engine.ExportQuality
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
        val isCancelled: Boolean = false,
        /** Features that could not be rendered exactly (never silently dropped). */
        val warnings: List<String> = emptyList()
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
        watermarkEnabled: Boolean = false,
        /** Requested video bitrate in bits per second, 0 = encoder default. */
        videoBitrateBps: Int = 0
    ): ExportOutput? = withContext(Dispatchers.Main) {
        val mediaClips = project.allVideoClips.filter { !it.uriString.isNullOrBlank() }
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
        val estimatedSize = estimateExportSize(
            project,
            target.width,
            target.height,
            target.fps,
            videoBitrateBps
        )
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

        // Build the exact same render graph the preview uses. A failure here is
        // surfaced to the user instead of silently exporting a plain, effects-
        // free video.
        val built = try {
            ProjectCompositionFactory.build(
                project = project,
                width = outWidth,
                height = outHeight,
                fps = target.fps,
                watermarkEnabled = watermarkEnabled
            )
        } catch (error: Throwable) {
            AppLog.error("ExportManager", "Failed to build the project render graph", error)
            _progress.value = ExportProgress(
                error = "Could not build the render graph: ${error.message ?: error.javaClass.simpleName}"
            )
            return@withContext null
        }
        built.warnings.forEach { warning ->
            AppLog.warn("ExportManager", warning)
        }
        if (built.warnings.isNotEmpty()) {
            _progress.value = _progress.value.copy(
                warnings = built.warnings,
                stage = "Rendering with ${built.warnings.size} compatibility note(s)..."
            )
        }

        val composition = built.composition
        var run = runTransformer(composition, outputFile, target.mime, videoBitrateBps)
        // If a pinned codec was rejected by the encoder, retry once with the
        // default codec path instead of failing the whole export.
        if (run.result == null && target.mime != null && isEncoderRelatedFailure(run.failure)) {
            AppLog.warn("ExportManager", "Codec ${target.mime} was rejected; retrying with the default encoder")
            _progress.value = _progress.value.copy(progress = 0f, stage = "Retrying with the default encoder...")
            run = runTransformer(composition, outputFile, null, videoBitrateBps)
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
            _progress.value = _progress.value.copy(
                progress = 0f,
                stage = "Export validation failed",
                error = validation.error ?: "Export validation failed",
                isCancelled = false,
                warnings = built.warnings
            )
            AppLog.error("ExportManager", "Export validation failed: ${validation.error}")
            return@withContext null
        }

        _progress.value = ExportProgress(
            progress = 1f,
            stage = "Export complete!",
            isDone = true,
            outputPath = outputFile.absolutePath,
            galleryUri = galleryUri,
            warnings = built.warnings
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
    /**
     * Estimates export size from the same bits-per-pixel ladder the export sheet
     * uses, so the storage pre-check and the UI estimate cannot disagree.
     */
    private fun estimateExportSize(
        project: VideoProject,
        width: Int,
        height: Int,
        fps: Int,
        videoBitrateBps: Int = 0
    ): Long {
        val safeFps = fps.coerceAtLeast(24)
        val preset = if (project.aspectRatio.ratioWidth <= project.aspectRatio.ratioHeight) {
            EngineExportPresets.portrait(
                width = width,
                height = height,
                quality = ExportQuality.HIGH,
                fps = safeFps
            )
        } else {
            EngineExportPresets.landscape(
                width = width,
                height = height,
                quality = ExportQuality.HIGH,
                fps = safeFps
            )
        }
        val effective = if (videoBitrateBps > 0) {
            preset.copy(videoBitrateKbps = (videoBitrateBps / 1_000).coerceAtLeast(1))
        } else {
            preset
        }
        // 1.1 safety margin covers container overhead and encoder variance.
        return (EngineExportPresets.estimateBytes(effective, project.totalDurationMs) * 1.1).toLong()
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
        videoMime: String? = null,
        videoBitrateBps: Int = 0
    ): TransformerRun = coroutineScope {
        var outcome = TransformerRun()

        // CapCut's Bitrate selector is a real encoder setting, not an estimate
        // label: pin the requested bitrate when the user picked one.
        val encoderFactory = if (videoBitrateBps > 0) {
            DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(videoBitrateBps)
                        .build()
                )
                .build()
        } else {
            null
        }

        val newTransformer = Transformer.Builder(context)
            .apply { videoMime?.let { mime -> setVideoMimeType(mime) } }
            .apply { encoderFactory?.let { factory -> setEncoderFactory(factory) } }
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
        val base = gain.coerceIn(0f, 2f)
        if (durationMs <= 0L) return base
        // Shared with the preview mixer (AudioMath) so fades match exactly.
        return base * AudioMath.fadeGain(
            positionMs = presentationTimeUs / 1000L,
            durationMs = durationMs,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs
        )
    }

    override fun isUnityUntil(presentationTimeUs: Long, sampleRate: Int): Long = Long.MIN_VALUE
}
