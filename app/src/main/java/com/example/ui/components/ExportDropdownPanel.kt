package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AspectRatioType
import com.example.viewmodel.EditorViewModel

/**
 * The CapCut-style dropdown body that expands under the editor top bar when
 * the user taps "AI UHD": video / GIF export settings. Selections live in
 * [EditorViewModel.ExportSettings], so the Export button next to the chip
 * starts the render with exactly what was pre-selected. The caller animates
 * the panel in/out (expand + fade) around this content.
 */
@Composable
internal fun ExportDropdownPanel(
    settings: EditorViewModel.ExportSettings,
    watermarkEnabled: Boolean,
    currentAspectRatio: AspectRatioType,
    projectLengthMs: Long,
    onUpdateSettings: (EditorViewModel.ExportSettings) -> Unit,
    onToggleWatermark: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWatermarkSettings by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(Color(0xFF141416))
            .border(
                1.dp,
                Color(0xFF262832),
                RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("export_dropdown_panel")
    ) {
        if (showWatermarkSettings) {
            ExportWatermarkPanel(
                watermarkEnabled = watermarkEnabled,
                watermarkPosition = settings.watermarkPosition,
                onToggleWatermark = onToggleWatermark,
                onUpdatePosition = { pos ->
                    onUpdateSettings(settings.copy(watermarkPosition = pos))
                },
                onBack = { showWatermarkSettings = false }
            )
        } else {
            ExportSettingsContent(
                settings = settings,
                watermarkEnabled = watermarkEnabled,
                currentAspectRatio = currentAspectRatio,
                projectLengthMs = projectLengthMs,
                onUpdateSettings = onUpdateSettings,
                onOpenWatermark = { showWatermarkSettings = true }
            )
        }
    }
}
