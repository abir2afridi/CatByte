package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AspectRatioType
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ExportUiState

/**
 * CapCut-style Export Dialog, Bottom Sheet Drawer, Fullscreen Exporting, and Quit Alert Dialog.
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
    var showQuitConfirmDialog by remember { mutableStateOf(false) }

    if (exportState.isExporting) {
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
                    if (showWatermarkSettings && !exportState.isCompleted) {
                        ExportWatermarkPanel(
                            watermarkEnabled = watermarkEnabled,
                            watermarkPosition = settings.watermarkPosition,
                            onToggleWatermark = onToggleWatermark,
                            onUpdatePosition = { pos ->
                                onUpdateSettings(settings.copy(watermarkPosition = pos))
                            },
                            onBack = { showWatermarkSettings = false }
                        )
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
                    } else if (exportState.error != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF4D4D).copy(alpha = 0.15f))
                                    .border(1.5.dp, Color(0xFFFF4D4D), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = "Export failed",
                                    tint = Color(0xFFFF4D4D),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Export failed",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                exportState.error,
                                color = Color(0xFFA5A8B2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
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

/**
 * CapCut-style Export Options Bottom Sheet Drawer (Screenshot 1).
 * Displays Save to TikTok, Save to Instagram, Save to Device cards, and watermark choices.
 */
@Composable
fun ExportOptionsBottomSheet(
    watermarkEnabled: Boolean,
    onToggleWatermark: (Boolean) -> Unit,
    onSaveToDevice: () -> Unit,
    onShareTikTok: () -> Unit,
    onShareInstagram: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var boostOnTikTok by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF141416))
            .padding(16.dp)
            .testTag("export_options_bottom_sheet")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(28.dp))
                Text(
                    text = "Export",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA5A8B2),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. TikTok Card (White background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { onShareTikTok() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "TikTok",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Save and share to TikTok",
                    color = Color(0xFF141416),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Instagram Card (Dark background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2026))
                    .clickable { onShareInstagram() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Instagram",
                    tint = Color(0xFFE1306C),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Save and share to Instagram",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Save to device Card (Dark background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2026))
                    .clickable { onSaveToDevice() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Save to device",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Save to device",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Radio options at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleWatermark(!watermarkEnabled) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(
                    selected = !watermarkEnabled,
                    onClick = { onToggleWatermark(!watermarkEnabled) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF26E5D7),
                        unselectedColor = Color(0xFFA5A8B2)
                    )
                )
                Text(
                    text = "Remove watermark",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { boostOnTikTok = !boostOnTikTok }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(
                    selected = boostOnTikTok,
                    onClick = { boostOnTikTok = !boostOnTikTok },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF26E5D7),
                        unselectedColor = Color(0xFFA5A8B2)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Boost your video on TikTok",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("ⓘ", color = Color(0xFFA5A8B2), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * CapCut-style Fullscreen Exporting Page (Screenshot 2).
 * Displays live progress percentage on center video preview.
 */
@Composable
fun ExportingScreen(
    progress: Float,
    stage: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .testTag("exporting_fullscreen_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with left back arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title & Subtitle
            Text(
                text = "Exporting...",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Please don't close CatByte or lock your screen.",
                color = Color(0xFFA5A8B2),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(50.dp))

            // Center Video Preview Card with Live Percentage
            Box(
                modifier = Modifier
                    .width(310.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181A20))
                    .border(1.dp, Color(0xFF2C2E38), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())

                // Dark subtle overlay for contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

                // Top thin white progress line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }

                // Center percentage text
                val percentageText = String.format("%.1f%%", progress * 100f)
                Text(
                    text = percentageText,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * CapCut-style Quit Exporting Confirmation Alert Dialog (Screenshot 3).
 */
@Composable
fun QuitExportingDialog(
    onContinue: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .testTag("quit_exporting_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quit exporting? You will go back to your project.",
                    color = Color(0xFF1E2026),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // "Continue exporting" cyan filled button
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(
                        text = "Continue exporting",
                        color = Color(0xFF041E21),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "Quit" plain text button
                Text(
                    text = "Quit",
                    color = Color(0xFF6E717C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onQuit() }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
