package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.AppLanguage
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip
import com.example.ui.theme.sketchPencilBorder
import com.example.viewmodel.ExportUiState
import androidx.compose.ui.platform.testTag

@Composable
fun ExportDialog(
    exportState: ExportUiState,
    defaultResolution: String = "1080P",
    defaultFps: Int = 30,
    watermarkEnabled: Boolean = false,
    onStartExport: (resolution: String, fps: Int, watermark: Boolean) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit,
    onShareExport: (platform: String) -> Unit
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current

    // Saveable state for resolution/FPS picker
    var selectedRes by rememberSaveable { mutableStateOf(defaultResolution) }
    var selectedFps by rememberSaveable { mutableIntStateOf(defaultFps) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .sketchPencilBorder(strokeWidth = 2.dp, color = colors.border, cornerRadius = 18.dp)
                .padding(20.dp)
                .testTag("export_dialog")
        ) {
            if (exportState.isExporting) {
                // Rendering Progress Mode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharpenedPencilTip(
                            modifier = Modifier.size(24.dp),
                            leadColor = colors.border,
                            woodColor = colors.accentYellow
                        )
                        Text("Drafting & Exporting Video...", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportState.stage, color = colors.accentCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(18.dp))
                    LinearProgressIndicator(
                        progress = { exportState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = colors.accentCyan,
                        trackColor = colors.surfaceElevated
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${(exportState.progress * 100).toInt()}%",
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Rendering frames with precision pencil lead", color = colors.textSecondary, fontSize = 11.sp)

                    // Cancel button during export
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCancelExport,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.accentGreen.copy(alpha = 0.2f))
                            .border(1.5.dp, colors.accentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = colors.accentGreen, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Masterpiece Ready!", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Rendered in ${exportState.resolution} @ ${exportState.fps}fps", color = colors.textSecondary, fontSize = 12.sp)

                    // Watermark note
                    if (watermarkEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "✏️ CatByte watermark applied",
                            color = colors.accentYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("TikTok" to colors.accentCyan, "Instagram" to colors.accentPink, "WhatsApp" to colors.accentGreen).forEach { (platform, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { onShareExport(platform) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = platform, tint = color, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(platform, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = if (colors.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Resolution & FPS Picker Mode
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📐", fontSize = 18.sp)
                        Text("Export Blueprint", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Resolution", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("720P", "1080P", "2K/4K").forEach { res ->
                            val isSelected = selectedRes == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.accentCyan else colors.surfaceElevated)
                                    .border(1.dp, if (isSelected) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { selectedRes = res }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res,
                                    color = if (isSelected) (if (colors.isLight) Color.White else Color.Black) else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Frame Rate", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(24 to "24 fps (Cinema)", 30 to "30 fps (Standard)", 60 to "60 fps (Smooth)").forEach { (fps, label) ->
                            val isSelected = selectedFps == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.accentCyan else colors.surfaceElevated)
                                    .border(1.dp, if (isSelected) colors.border else colors.borderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${fps}fps",
                                    color = if (isSelected) (if (colors.isLight) Color.White else Color.Black) else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Watermark preview toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceVariant)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add CatByte Watermark",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Subtle sketch logo on exported video",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                        // Note: this is preview only, actual watermark is passed via onStartExport
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = colors.textPrimary)
                        }
                        Button(
                            onClick = { onStartExport(selectedRes, selectedFps, watermarkEnabled) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export 🚀", color = if (colors.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}