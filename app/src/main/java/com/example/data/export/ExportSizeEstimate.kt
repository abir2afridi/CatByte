package com.example.data.export

import com.example.engine.ExportPresets as EngineExportPresets
import com.example.engine.ExportQuality
import com.example.model.VideoProject

/**
 * Storage pre-check for an export.
 *
 * The estimate uses the same bits-per-pixel ladder the export sheet uses, so
 * the storage pre-check and the UI estimate can never disagree.
 */

/**
 * Estimates export file size: bitrate × duration × 1.1 safety margin.
 * Bitrate is chosen from the real output size (post capability downgrade).
 */
internal fun estimateExportSize(
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
