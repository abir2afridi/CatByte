package com.example.ui.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportResolution
import com.example.engine.ExportContainer
import com.example.engine.ExportPreset
import com.example.engine.ExportPresets
import com.example.model.AspectRatioType
import com.example.export.ExportSettings
import kotlin.math.roundToInt

/**
 * "Estimated file size: N MB" footer under the export settings.
 *
 * The estimate is bitrate-driven (same math as ExportPresets.estimateMb), so
 * changing the resolution alone does NOT change the estimate - only bitrate
 * and length do.
 */
@Composable
internal fun ExportSizeEstimateFooter(
    settings: ExportSettings,
    currentAspectRatio: AspectRatioType,
    projectLengthMs: Long,
    modifier: Modifier = Modifier
) {
    val estimateMb = rememberEstimateMb(settings, currentAspectRatio, projectLengthMs)
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "Estimated file size: ${estimateMb.roundToInt()} MB",
            color = Color(0xFFA5A8B2),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** File-size estimate for the currently selected video settings. */
@Composable
private fun rememberEstimateMb(
    settings: ExportSettings,
    currentAspectRatio: AspectRatioType,
    projectLengthMs: Long
): Double {
    val (w, h, mb) = remember(
        currentAspectRatio, settings.resolution, settings.fps, settings.bitrateMbps, projectLengthMs
    ) {
        val (width, height) = ExportResolution.outputSize(currentAspectRatio, settings.resolution)
        val preset = ExportPreset(
            id = "estimate",
            displayName = "Estimate",
            container = ExportContainer.MP4_H264,
            width = width,
            height = height,
            fps = settings.fps,
            videoBitrateKbps = settings.bitrateMbps * 1000,
            audioBitrateKbps = 128,
            platform = "Estimate"
        )
        Triple(width, height, ExportPresets.estimateMb(preset, projectLengthMs))
    }
    return mb
}
