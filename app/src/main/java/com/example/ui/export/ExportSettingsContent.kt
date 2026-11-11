package com.example.ui.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.export.ExportSettings
import com.example.model.AspectRatioType

/**
 * The CapCut-style export settings body: routes to the video or GIF feature
 * set based on the active tab. Each feature lives in its own file so a bug in
 * a specific control is a one-file hunt:
 *
 * - `ExportAiUhdCard.kt`        - AI ultra HD card
 * - `ExportResolutionSelector.kt` - resolution stops
 * - `ExportFrameRateSelector.kt`  - frame rate stops
 * - `ExportOpticalFlowRow.kt`     - optical flow row
 * - `ExportBitrateSelector.kt`    - bitrate stops
 * - `ExportGifQualityCards.kt`    - GIF quality cards
 * - `ExportWatermarkRow.kt` / `ExportWatermarkPanel.kt` - watermark
 * - `ExportSizeEstimate.kt`       - estimated file size footer
 */
@Composable
internal fun ExportSettingsContent(
    settings: ExportSettings,
    watermarkEnabled: Boolean,
    currentAspectRatio: AspectRatioType,
    projectLengthMs: Long,
    onUpdateSettings: (ExportSettings) -> Unit,
    onOpenWatermark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (settings.tab == "video") {
            // ---- Video tab ----
            ExportAiUhdCard()
            Spacer(modifier = Modifier.height(14.dp))

            ExportResolutionSelector(
                selectedResolution = settings.resolution,
                onSelect = { resolution ->
                    onUpdateSettings(settings.copy(resolution = resolution))
                }
            )
            Spacer(modifier = Modifier.height(14.dp))

            ExportFrameRateSelector(
                selectedFps = settings.fps,
                onSelect = { fps -> onUpdateSettings(settings.copy(fps = fps)) }
            )
            Spacer(modifier = Modifier.height(14.dp))

            ExportOpticalFlowRow()
            Spacer(modifier = Modifier.height(14.dp))

            ExportBitrateSelector(
                selectedBitrateMbps = settings.bitrateMbps,
                onSelect = { bitrateMbps ->
                    onUpdateSettings(settings.copy(bitrateMbps = bitrateMbps))
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            ExportWatermarkRow(
                watermarkEnabled = watermarkEnabled,
                onClick = onOpenWatermark
            )
            Spacer(modifier = Modifier.height(14.dp))

            ExportSizeEstimateFooter(
                settings = settings,
                currentAspectRatio = currentAspectRatio,
                projectLengthMs = projectLengthMs
            )
        } else {
            // ---- GIF tab ----
            ExportGifQualityCards(
                selectedQuality = settings.gifQuality,
                onSelect = { quality ->
                    onUpdateSettings(settings.copy(gifQuality = quality))
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExportWatermarkRow(
                watermarkEnabled = watermarkEnabled,
                onClick = onOpenWatermark
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExportGifSizeNote()
        }
    }
}
