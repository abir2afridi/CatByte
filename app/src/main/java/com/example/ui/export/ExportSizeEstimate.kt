package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val mbText = if (estimateMb < 1.0) "< 1 MB" else "~${estimateMb.roundToInt()} MB"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B1C22))
            .border(0.8.dp, Color(0xFF2C2E38), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💾 Estimated file size:",
                color = Color(0xFFA5A8B2),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = mbText,
                color = Color(0xFF26E5D7),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "•",
                color = Color(0xFF555765),
                fontSize = 10.sp
            )
            Text(
                text = "${settings.resolution} ${settings.fps}fps",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
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
