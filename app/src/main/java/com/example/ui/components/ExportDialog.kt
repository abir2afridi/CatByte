package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AspectRatioType
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ExportUiState

/**
 * The export dialog. Its SETTINGS body moved to [ExportSettingsContent] and is
 * shared with the AI UHD dropdown in the top bar; this dialog now mainly hosts
 * the render progress and completion states (one-click Export from the top bar
 * jumps straight here), plus the full settings page for deep links.
 */
@Composable
fun ExportDialog(
    exportState: ExportUiState,
    settings: EditorViewModel.ExportSettings,
    onUpdateSettings: (EditorViewModel.ExportSettings) -> Unit,
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
                if (showWatermarkSettings && !exportState.isExporting && !exportState.isCompleted) {
                    // Watermark Sub-Panel
                    ExportWatermarkPanel(
                        watermarkEnabled = watermarkEnabled,
                        watermarkPosition = settings.watermarkPosition,
                        onToggleWatermark = onToggleWatermark,
                        onUpdatePosition = { pos ->
                        onUpdateSettings(settings.copy(watermarkPosition = pos))
                    },
                        onBack = { showWatermarkSettings = false }
                    )
                } else if (exportState.isExporting) {
                    // Rendering Progress Mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Exporting",
                                tint = Color(0xFF26E5D7),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Exporting Video...",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            exportState.stage,
                            color = Color(0xFF26E5D7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFF26E5D7),
                            trackColor = Color(0xFF22242B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "${(exportState.progress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCancelExport,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Export", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (exportState.isCompleted) {
                    // Completed Mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                .border(1.5.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Masterpiece Ready!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Settings page (deep links / feature search). The top-bar
                    // dropdown is the primary entry; this hosts the same body
                    // with its own header row.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorExportTabHeader(
                                    label = "video",
                                    selected = settings.tab == "video",
                                    onClick = { onUpdateSettings(settings.copy(tab = "video")) }
                                )
                                EditorExportTabHeader(
                                    label = "GIF",
                                    selected = settings.tab == "gif",
                                    onClick = { onUpdateSettings(settings.copy(tab = "gif")) }
                                )
                            }

                            Row(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF24252A))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (settings.tab == "video") "AI UHD" else "GIF",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Dropdown",
                                        tint = Color(0xFFA5A8B2),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Button(
                                    onClick = onStartSelectedExport,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        "Export",
                                        color = Color(0xFF041E21),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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

/** One "video"/"GIF" tab header with the cyan underline when selected. */
@Composable
private fun EditorExportTabHeader(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFA5A8B2),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.5.dp)
                    .background(Color(0xFF26E5D7), RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(4.5.dp))
        }
    }
}
