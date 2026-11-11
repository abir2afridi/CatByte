package com.example.ui.export

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.export.ExportSettings
import com.example.export.ExportUiState
import com.example.model.AspectRatioType

/**
 * Host for every export UI state, routed by [ExportUiState]:
 *
 * - completed -> [ExportDoneScreen]   (fullscreen result, share/save)
 * - exporting -> [ExportingScreen]    (fullscreen progress) + [QuitExportingDialog]
 * - failed    -> [ExportErrorSection] inside the settings dialog
 * - otherwise -> the export settings ([ExportSettingsContent] / watermark panel)
 *
 * The editor renders this at its root Box level so it never depends on a
 * bottom tool sheet being open (that nesting bug made the whole export flow
 * invisible - see commit 7f3e718).
 */
@Composable
fun ExportDialog(
    exportState: ExportUiState,
    settings: ExportSettings,
    onUpdateSettings: (ExportSettings) -> Unit,
    watermarkEnabled: Boolean = false,
    currentAspectRatio: AspectRatioType = AspectRatioType.RATIO_16_9,
    projectLengthMs: Long = 60_000L,
    onToggleWatermark: (Boolean) -> Unit = {},
    onStartSelectedExport: () -> Unit = {},
    onCancelExport: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onShareExport: (platform: String) -> Unit = {}
) {
    var showWatermarkSettings by remember { mutableStateOf(false) }
    var showQuitConfirmDialog by remember { mutableStateOf(false) }

    if (exportState.isCompleted) {
        // Fullscreen Export Done Screen (Screenshot 4)
        ExportDoneScreen(
            videoPath = exportState.videoPath,
            onDone = onDismiss,
            onSharePlatform = onShareExport
        )
    } else if (exportState.isExporting) {
        // Fullscreen Exporting Page (Screenshot 2)
        ExportingScreen(
            progress = exportState.progress,
            stage = exportState.stage,
            onBackClick = { showQuitConfirmDialog = true }
        )

        // Quit Exporting Confirmation Dialog (Screenshot 3)
        if (showQuitConfirmDialog) {
            QuitExportingDialog(
                onContinue = { showQuitConfirmDialog = false },
                onQuit = {
                    showQuitConfirmDialog = false
                    onCancelExport()
                }
            )
        }
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 0.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0xFF262832), RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                        .testTag("export_dialog")
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
                    } else if (exportState.error != null) {
                        ExportErrorSection(
                            error = exportState.error,
                            onDismiss = onDismiss
                        )
                    } else {
                        // Settings Body
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
        }
    }
}
