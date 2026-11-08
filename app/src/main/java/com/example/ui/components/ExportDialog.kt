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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.CanvasMath
import com.example.engine.ExportContainer
import com.example.engine.ExportPreset
import com.example.engine.ExportPresets
import com.example.model.AspectRatioType
import com.example.ui.theme.PencilTheme
import com.example.viewmodel.ExportUiState
import kotlin.math.roundToInt

@Composable
fun ExportDialog(
    exportState: ExportUiState,
    defaultResolution: String = "1080P",
    defaultFps: Int = 30,
    watermarkEnabled: Boolean = false,
    currentAspectRatio: AspectRatioType = AspectRatioType.RATIO_16_9,
    projectLengthMs: Long = 60_000L,
    onSelectAspectRatio: (AspectRatioType) -> Unit = {},
    onToggleWatermark: (Boolean) -> Unit = {},
    onStartExport: (resolution: String, fps: Int, watermark: Boolean, bitrateMbps: Int) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit,
    onShareExport: (platform: String) -> Unit,
    onStartBackgroundExport: (resolution: String, fps: Int, watermark: Boolean, bitrateMbps: Int) -> Unit =
        { _, _, _, _ -> }
) {
    val colors = PencilTheme.colors
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableStateOf("video") }
    var selectedRes by rememberSaveable { mutableStateOf(defaultResolution) }
    var selectedFps by rememberSaveable { mutableIntStateOf(defaultFps) }
    var selectedBitrate by rememberSaveable { mutableIntStateOf(10) }
    var gifQuality by rememberSaveable { mutableStateOf("240P") }
    var showWatermarkSettings by rememberSaveable { mutableStateOf(false) }
    var watermarkPosition by rememberSaveable { mutableStateOf("Upper left") }

    val estimate = remember(currentAspectRatio, selectedRes, selectedFps, selectedBitrate, projectLengthMs) {
        val (w, h) = CanvasMath.canvasSize(currentAspectRatio, selectedRes)
        val preset = ExportPreset(
            id = "estimate",
            displayName = "Estimate",
            container = ExportContainer.MP4_H264,
            width = w,
            height = h,
            fps = selectedFps,
            videoBitrateKbps = selectedBitrate * 1000,
            audioBitrateKbps = 128,
            platform = "Estimate"
        )
        Triple(w, h, ExportPresets.estimateMb(preset, projectLengthMs))
    }
    val (estWidth, estHeight, estMb) = estimate

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
                    // Watermark Sub-Panel (Screenshot 3)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showWatermarkSettings = false }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Watermark", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Video preview with Watermark position
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1E24)),
                            contentAlignment = Alignment.Center
                        ) {
                            CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())

                            val alignment = when (watermarkPosition) {
                                "Upper left" -> Alignment.TopStart
                                "Upper right" -> Alignment.TopEnd
                                "Lower left" -> Alignment.BottomStart
                                else -> Alignment.BottomEnd
                            }
                            Text(
                                text = "CapCut",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(alignment)
                                    .padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Watermark: CapCut", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onToggleWatermark(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (watermarkEnabled) Color(0xFF2A2B34) else Color(0xFF1E2026)
                                ),
                                border = if (watermarkEnabled) androidx.compose.foundation.BorderStroke(1.dp, Color.White) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Text("Keep", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onToggleWatermark(false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!watermarkEnabled) Color(0xFF2A2B34) else Color(0xFF1E2026)
                                ),
                                border = if (!watermarkEnabled) androidx.compose.foundation.BorderStroke(1.dp, Color.White) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Position", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val positions = listOf("Upper left", "Upper right", "Lower left", "Lower right")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(positions[0], positions[1]).forEach { pos ->
                                    val sel = watermarkPosition == pos
                                    Button(
                                        onClick = { watermarkPosition = pos },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (sel) Color(0xFF2A2B34) else Color(0xFF1E2026)),
                                        border = if (sel) androidx.compose.foundation.BorderStroke(1.dp, Color.White) else null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text(pos, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(positions[2], positions[3]).forEach { pos ->
                                    val sel = watermarkPosition == pos
                                    Button(
                                        onClick = { watermarkPosition = pos },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (sel) Color(0xFF2A2B34) else Color(0xFF1E2026)),
                                        border = if (sel) androidx.compose.foundation.BorderStroke(1.dp, Color.White) else null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text(pos, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else if (exportState.isExporting) {
                    // Rendering Progress Mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Exporting",
                                tint = Color(0xFF26E5D7),
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Exporting Video...", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(exportState.stage, color = Color(0xFF26E5D7), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(18.dp))
                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFF26E5D7),
                            trackColor = Color(0xFF22242B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("${(exportState.progress * 100).toInt()}%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
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
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.2f)).border(1.5.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Masterpiece Ready!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    // Dropdown Export Settings Panel (Screenshot 1 & 2)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Header Row (video / GIF tabs + AI UHD / GIF chip + Export button)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.clickable { selectedTab = "video" },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "video",
                                        color = if (selectedTab == "video") Color.White else Color(0xFFA5A8B2),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (selectedTab == "video") {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(2.5.dp)
                                                .background(Color(0xFF26E5D7), RoundedCornerShape(1.dp))
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.clickable { selectedTab = "gif" },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "GIF",
                                        color = if (selectedTab == "gif") Color.White else Color(0xFFA5A8B2),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (selectedTab == "gif") {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(Color(0xFF26E5D7), RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chip button (AI UHD ▴ or GIF ▴)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF24252A))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedTab == "video") "AI UHD" else "GIF",
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

                                // Export Button (Starts Export!)
                                Button(
                                    onClick = {
                                        onStartExport(selectedRes, selectedFps, watermarkEnabled, selectedBitrate)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7)),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Export", color = Color(0xFF041E21), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedTab == "video") {
                            // Video Tab Options (Screenshot 1)

                            // AI Ultra HD Toggle Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E2026))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("AI ultra HD", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("| ▶ Example", color = Color(0xFFA5A8B2), fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Make your video clearer and smoother with AI enhancement and frame interpolation.",
                                        color = Color(0xFFA5A8B2),
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(checked = false, onCheckedChange = null, enabled = false)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Resolution Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Resolution", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("?", color = Color(0xFFA5A8B2), fontSize = 11.sp)
                                }
                                Text("Standard definition - the resolution used for most TikTok videos", color = Color(0xFFA5A8B2), fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ExportSelectorTrack(
                                labels = listOf("480p", "720p", "1080p", "2k", "4k"),
                                selectedIndex = listOf("480P", "720P", "1080P", "2K", "4K")
                                    .indexOf(selectedRes)
                                    .coerceAtLeast(1),
                                onSelect = { index ->
                                    selectedRes = listOf("480P", "720P", "1080P", "2K", "4K")[index]
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Frame Rate Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Frame rate", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Smoother playback", color = Color(0xFFA5A8B2), fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ExportSelectorTrack(
                                labels = listOf("24", "25", "30", "50", "60"),
                                selectedIndex = listOf(24, 25, 30, 50, 60)
                                    .indexOf(selectedFps)
                                    .coerceAtLeast(2),
                                onSelect = { index ->
                                    selectedFps = listOf(24, 25, 30, 50, 60)[index]
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Optical flow Toggle Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("Optical flow", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Make video playback smoother.", color = Color(0xFF26E5D7), fontSize = 10.sp)
                                        Text("| ▶ Example", color = Color(0xFFA5A8B2), fontSize = 10.sp)
                                    }
                                }
                                Switch(checked = false, onCheckedChange = null, enabled = false)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bitrate (Mbps) Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bitrate (Mbps)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Recommended for this video(10)", color = Color(0xFFA5A8B2), fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ExportSelectorTrack(
                                labels = listOf("5", "10", "20", "50", "100"),
                                selectedIndex = listOf(5, 10, 20, 50, 100)
                                    .indexOf(selectedBitrate)
                                    .coerceAtLeast(1),
                                onSelect = { index ->
                                    selectedBitrate = listOf(5, 10, 20, 50, 100)[index]
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Watermark Row (opens Watermark sub-panel)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWatermarkSettings = true }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Watermark", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (watermarkEnabled) "CapCut" else "None", color = Color(0xFFA5A8B2), fontSize = 12.sp)
                                    Text(">", color = Color(0xFFA5A8B2), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Estimated file size footer
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Estimated file size: ${estMb.roundToInt()} MB",
                                    color = Color(0xFFA5A8B2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // GIF Tab Options (Screenshot 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    Triple("240P", "Standard", "240P"),
                                    Triple("320P", "High", "320P"),
                                    Triple("640P", "Ultra high", "640P")
                                ).forEach { (res, sub, id) ->
                                    val sel = gifQuality == id
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E2026))
                                            .border(
                                                width = if (sel) 1.5.dp else 0.8.dp,
                                                color = if (sel) Color.White else Color(0xFF2D303B),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { gifQuality = id }
                                            .padding(8.dp)
                                    ) {
                                        Text(res, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2D303B)))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(sub, color = Color(0xFFA5A8B2), fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Watermark Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWatermarkSettings = true }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Watermark", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (watermarkEnabled) "CapCut" else "None", color = Color(0xFFA5A8B2), fontSize = 12.sp)
                                    Text(">", color = Color(0xFFA5A8B2), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // GIF estimated size footer
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Size of exported GIF is about 90MB",
                                    color = Color(0xFFA5A8B2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * CapCut export selectors are horizontal tracks with a dot and label per stop.
 */
@Composable
private fun ExportSelectorTrack(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val y = 6.dp.toPx()
                val inset = size.width / (labels.size * 2f)
                drawLine(
                    color = Color(0xFF2D303B),
                    start = Offset(inset, y),
                    end = Offset(size.width - inset, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 0.dp)
                    .testTag("export_selector_$label")
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color(0xFF2D303B))
                        .border(
                            width = 1.dp,
                            color = if (selected) Color.White else Color(0xFF3D404D),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFFA5A8B2),
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
