package com.example.data.export

import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import com.example.data.AppLog
import com.example.data.ExportManager
import com.example.data.MediaStoreSaver
import com.example.data.ProjectCompositionFactory
import com.example.export.ExportOutput
import com.example.export.ExportProgress
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The export pipeline: capability check, storage pre-check, render graph
 * build, render, gallery hand-off and validation.
 *
 * Everything the user sees in the preview is applied to the exported file:
 * filters, colour adjustments, rotation / mirroring, transitions, speed,
 * per-clip volume, text and sticker overlays and the watermark.
 */
@UnstableApi
@androidx.annotation.OptIn(ExperimentalApi::class)
internal suspend fun ExportManager.runExportPipeline(
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
    val availableSpace = appContext.getExternalFilesDir(null)?.usableSpace ?: 0L
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

    val outputDir = File(appContext.getExternalFilesDir(null), "exports").apply { mkdirs() }
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
        MediaStoreSaver.saveVideo(
            appContext,
            outputFile,
            "CatByte_${project.title.take(24).replace("/", "-")}"
        )
    }.getOrNull()

    // Validate exported MP4 before reporting success
    val validation = validateExportFile(outputFile, project)
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
