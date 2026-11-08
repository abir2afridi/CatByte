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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.engine.CanvasMath
import com.example.engine.ExportContainer
import com.example.engine.ExportPreset
import com.example.engine.ExportPresets
import com.example.engine.ExportQuality
import com.example.model.AspectRatioType
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip
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
                    .background(colors.surface)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .testTag("export_dialog")
            ) {
                if (exportState.isExporting) {
                    // Rendering Progress Mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (colors.isPencilSketch) {
                                SharpenedPencilTip(
                                    modifier = Modifier.size(24.dp),
                                    leadColor = colors.border,
                                    woodColor = colors.accentYellow
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Exporting",
                                    tint = colors.accentCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text("Exporting Video...", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(exportState.stage, color = colors.accentCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (exportState.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exportState.warnings.first(),
                                color = colors.accentYellow,
                                fontSize = 10.sp,
                                maxLines = 2
                            )
                        }

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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
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

                        Spacer(modifier = Modifier.height(18.dp))

                        val videoFile = java.io.File(exportState.videoPath)
                        val installed = com.example.data.ShareHelper.installedTargets(context)
                        val shareTargets = if (installed.isNotEmpty()) {
                            installed
                        } else {
                            listOf(
                                com.example.data.ShareHelper.ShareTarget("Share…", "", "📤")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            shareTargets.forEach { target ->
                                val color = when (target.label) {
                                    "TikTok" -> colors.accentCyan
                                    "Instagram" -> colors.accentPink
                                    "WhatsApp" -> colors.accentGreen
                                    else -> colors.accentPurple
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (videoFile.exists()) {
                                                val intent = if (target.packageName.isNotEmpty()) {
                                                    com.example.data.ShareHelper.buildShareIntent(context, videoFile, target)
                                                } else {
                                                    Intent.createChooser(
                                                        com.example.data.ShareHelper.buildShareIntent(context, videoFile),
                                                        "Share to"
                                                    )
                                                }
                                                runCatching { context.startActivity(intent) }
                                            }
                                            onShareExport(target.label)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = target.label, tint = color, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(target.label, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                onStartBackgroundExport(selectedRes, selectedFps, watermarkEnabled, selectedBitrate)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export in Background", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // CapCut Export Sheet Mode matching the screenshot
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Header Row (video / GIF tabs + AI UHD + Export button)
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
                                        color = if (selectedTab == "video") colors.textPrimary else colors.textTertiary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (selectedTab == "video") {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.5.dp)
                                                .background(colors.accentCyan, RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.clickable { selectedTab = "gif" },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "GIF",
                                        color = if (selectedTab == "gif") colors.textPrimary else colors.textTertiary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (selectedTab == "gif") {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(colors.accentCyan, RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // AI UHD dropdown button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("AI UHD ▾", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Export Button. GIF is shown for parity but the
                                // offline encoder only writes MP4, so it stays
                                // disabled instead of silently exporting video.
                                Button(
                                    onClick = {
                                        if (selectedTab == "video") {
                                            onStartExport(
                                                selectedRes,
                                                selectedFps,
                                                watermarkEnabled,
                                                selectedBitrate
                                            )
                                        }
                                    },
                                    enabled = selectedTab == "video",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accentCyan,
                                        disabledContainerColor = colors.surfaceElevated,
                                        disabledContentColor = colors.textTertiary
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Export", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedTab == "gif") {
                            Text(
                                text = "GIF export needs a GIF encoder that isn't bundled with this offline build. Switch to the video tab to export MP4.",
                                color = colors.accentYellow,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.accentYellow.copy(alpha = 0.10f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // AI Ultra HD Toggle Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceElevated)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("AI ultra HD 💎", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Needs an online AI model — this offline build exports with the standard encoder.",
                                    color = colors.textTertiary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⊡ Example",
                                    color = colors.accentCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = false,
                                onCheckedChange = null,
                                enabled = false
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Resolution Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Resolution", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("High definition - the ultimate viewing experience", color = colors.textTertiary, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ExportSelectorTrack(
                            labels = listOf("480p", "540p", "720p", "1080p", "2K/4K"),
                            selectedIndex = listOf("480P", "540P", "720P", "1080P", "4K")
                                .indexOf(selectedRes)
                                .coerceAtLeast(0),
                            onSelect = { index ->
                                selectedRes = listOf("480P", "540P", "720P", "1080P", "4K")[index]
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Frame Rate Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Frame rate", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Smoother playback", color = colors.textTertiary, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ExportSelectorTrack(
                            labels = listOf("24", "25", "30", "50", "60"),
                            selectedIndex = listOf(24, 25, 30, 50, 60)
                                .indexOf(selectedFps)
                                .coerceAtLeast(0),
                            onSelect = { index ->
                                selectedFps = listOf(24, 25, 30, 50, 60)[index]
                            }
                        )

                        // Bitrate Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bitrate (Mbps)", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Recommended for this video(10)", color = colors.textTertiary, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ExportSelectorTrack(
                            labels = listOf("5", "10", "20", "50", "100"),
                            selectedIndex = listOf(5, 10, 20, 50, 100)
                                .indexOf(selectedBitrate)
                                .coerceAtLeast(0),
                            onSelect = { index ->
                                selectedBitrate = listOf(5, 10, 20, 50, 100)[index]
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Optical flow: surface honestly, since frame
                        // interpolation needs the GPU pipeline.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Optical flow", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Pro",
                                        color = colors.accentYellow,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.accentYellow.copy(alpha = 0.14f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    "Frame interpolation needs the GPU export pipeline (not available offline).",
                                    color = colors.textTertiary,
                                    fontSize = 9.sp
                                )
                            }
                            Switch(checked = false, onCheckedChange = null, enabled = false)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Watermark row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleWatermark(!watermarkEnabled) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Watermark", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (watermarkEnabled) "CatByte" else "None", color = colors.textSecondary, fontSize = 12.sp)
                                Text(">", color = colors.textTertiary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Estimated file size footer
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Estimated file size: ${estMb.roundToInt()} MB",
                                color = colors.textTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * CapCut export selectors are sliders, not chip grids: a horizontal track with
 * a dot and label per stop. This composable keeps that exact shape for the
 * Resolution, Frame rate and Bitrate rows.
 */
@Composable
private fun ExportSelectorTrack(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val y = 6.dp.toPx()
                val inset = size.width / (labels.size * 2f)
                drawLine(
                    color = colors.borderSubtle,
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
                        .background(if (selected) colors.accentCyan else colors.surfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (selected) colors.accentCyan else colors.borderSubtle,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = label,
                    color = if (selected) colors.accentCyan else colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
