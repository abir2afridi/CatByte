package com.example.data.export

import android.media.MediaMetadataRetriever
import com.example.data.AppLog
import com.example.data.ExportManager
import com.example.model.VideoProject
import java.io.File

/**
 * Validation of a rendered MP4 before it is reported to the user as a success.
 *
 * A Transformer can "complete" and still hand back a broken file (empty
 * container, missing video track, a duration that does not match the
 * timeline). Reporting that as "Export complete" would be a lie, so the
 * finished file is inspected with MediaMetadataRetriever first.
 */

/**
 * Validates an exported MP4 file using MediaMetadataRetriever.
 * Checks: file exists, has video track, duration within tolerance.
 */
internal fun ExportManager.validateExportFile(
    outputFile: File,
    project: VideoProject
): ExportManager.ValidationResult {
    val file = outputFile
    if (!file.exists()) {
        return ExportManager.ValidationResult(
            false,
            "Exported file does not exist: ${outputFile.absolutePath}"
        )
    }
    val fileSize = file.length()
    if (fileSize == 0L) {
        return ExportManager.ValidationResult(false, "Exported file is empty: ${outputFile.absolutePath}")
    }
    AppLog.info("ExportManager", "Validating export: ${outputFile.name} ($fileSize bytes)")

    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(outputFile.absolutePath)

        // Check video track dimensions
        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        if (widthStr == null || heightStr == null) {
            return ExportManager.ValidationResult(false, "Exported video has no video track dimensions")
        }
        val width = widthStr.toIntOrNull() ?: 0
        val height = heightStr.toIntOrNull() ?: 0
        if (width <= 0 || height <= 0) {
            return ExportManager.ValidationResult(false, "Invalid video dimensions: ${width}x$height")
        }

        // Check duration
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        val expectedDurationMs = project.totalDurationMs
        val durationDiff = kotlin.math.abs(durationMs - expectedDurationMs)
        if (durationMs == 0L) {
            return ExportManager.ValidationResult(false, "Exported video has zero duration")
        }
        if (durationDiff > 1000L) { // Allow 1 second tolerance
            return ExportManager.ValidationResult(
                false,
                "Duration mismatch: expected ${expectedDurationMs}ms, got ${durationMs}ms (diff ${durationDiff}ms)"
            )
        }

        AppLog.info(
            "ExportManager",
            "Export validation PASSED: ${outputFile.name} (${width}x${height}, ${durationMs}ms)"
        )
        ExportManager.ValidationResult(true, null)
    }.getOrElse { e ->
        AppLog.warn("ExportManager", "Export validation failed for $outputFile", e)
        ExportManager.ValidationResult(
            false,
            "Validation error: ${e.message ?: e.javaClass.simpleName}"
        )
    }
}
