package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CanvasMath
import com.example.engine.ExportPreset
import com.example.engine.ExportContainer
import com.example.engine.ExportPresets
import com.example.model.AspectRatioType
import com.example.viewmodel.EditorViewModel
import kotlin.math.roundToInt

/**
 * The CapCut-style export settings body shared by the AI UHD dropdown in the
 * editor top bar and the export dialog: video options (AI ultra HD, resolution,
 * frame rate, optical flow, bitrate, watermark) or the GIF quality cards.
 * Selection state lives in [EditorViewModel.ExportSettings] so a single click
 * on Export renders exactly what the user pre-selected.
 */
@Composable
internal fun ExportSettingsContent(
    settings: EditorViewModel.ExportSettings,
    watermarkEnabled: Boolean,
    currentAspectRatio: AspectRatioType,
    projectLengthMs: Long,
    onUpdateSettings: (EditorViewModel.ExportSettings) -> Unit,
    onOpenWatermark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (settings.tab == "video") {
            // ---- Video tab ----

            // AI ultra HD toggle card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("AI ultra HD", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("| Example", color = Color(0xFFA5A8B2), fontSize = 10.sp)
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

            // Resolution slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Resolution", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("?", color = Color(0xFFA5A8B2), fontSize = 11.sp)
                }
                Text(
                    "Standard definition - the resolution used for most TikTok videos",
                    color = Color(0xFFA5A8B2),
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val resolutions = listOf("480P", "720P", "1080P", "2K", "4K")
            ExportSelectorTrack(
                labels = listOf("480p", "720p", "1080p", "2k", "4k"),
                selectedIndex = resolutions.indexOf(settings.resolution).coerceAtLeast(1),
                onSelect = { index ->
                    onUpdateSettings(settings.copy(resolution = resolutions[index]))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Frame rate slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Frame rate", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Smoother playback", color = Color(0xFFA5A8B2), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            val frameRates = listOf(24, 25, 30, 50, 60)
            ExportSelectorTrack(
                labels = listOf("24", "25", "30", "50", "60"),
                selectedIndex = frameRates.indexOf(settings.fps).coerceAtLeast(2),
                onSelect = { index ->
                    onUpdateSettings(settings.copy(fps = frameRates[index]))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Optical flow toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Optical flow", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Make video playback smoother.", color = Color(0xFF26E5D7), fontSize = 10.sp)
                        Text("| Example", color = Color(0xFFA5A8B2), fontSize = 10.sp)
                    }
                }
                Switch(checked = false, onCheckedChange = null, enabled = false)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bitrate (Mbps) slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bitrate (Mbps)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Recommended for this video(10)", color = Color(0xFFA5A8B2), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            val bitrates = listOf(5, 10, 20, 50, 100)
            ExportSelectorTrack(
                labels = listOf("5", "10", "20", "50", "100"),
                selectedIndex = bitrates.indexOf(settings.bitrateMbps).coerceAtLeast(1),
                onSelect = { index ->
                    onUpdateSettings(settings.copy(bitrateMbps = bitrates[index]))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExportWatermarkRow(
                watermarkEnabled = watermarkEnabled,
                onClick = onOpenWatermark
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Estimated file size footer
            val estimateMb = rememberEstimateMb(settings, currentAspectRatio, projectLengthMs)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Estimated file size: ${estimateMb.roundToInt()} MB",
                    color = Color(0xFFA5A8B2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // ---- GIF tab ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple("240P", "Standard", "240P"),
                    Triple("320P", "High", "320P"),
                    Triple("640P", "Ultra high", "640P")
                ).forEach { (res, sub, id) ->
                    val sel = settings.gifQuality == id
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
                            .clickable { onUpdateSettings(settings.copy(gifQuality = id)) }
                            .padding(8.dp)
                    ) {
                        Text(res, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF2D303B))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(sub, color = Color(0xFFA5A8B2), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExportWatermarkRow(
                watermarkEnabled = watermarkEnabled,
                onClick = onOpenWatermark
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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

/** "Watermark  CapCut >" row shared by both tabs. */
@Composable
private fun ExportWatermarkRow(
    watermarkEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Watermark", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (watermarkEnabled) "CapCut" else "None",
                color = Color(0xFFA5A8B2),
                fontSize = 12.sp
            )
            Text(">", color = Color(0xFFA5A8B2), fontSize = 12.sp)
        }
    }
}

/** The watermark sub-panel (preview + Keep/Remove + position grid). */
@Composable
internal fun ExportWatermarkPanel(
    watermarkEnabled: Boolean,
    watermarkPosition: String,
    onToggleWatermark: (Boolean) -> Unit,
    onUpdatePosition: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Watermark", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                border = if (watermarkEnabled) {
                    androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                } else {
                    null
                },
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
                border = if (!watermarkEnabled) {
                    androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                } else {
                    null
                },
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
                        onClick = { onUpdatePosition(pos) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sel) Color(0xFF2A2B34) else Color(0xFF1E2026)
                        ),
                        border = if (sel) {
                            androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        } else {
                            null
                        },
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
                        onClick = { onUpdatePosition(pos) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sel) Color(0xFF2A2B34) else Color(0xFF1E2026)
                        ),
                        border = if (sel) {
                            androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(pos, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** File-size estimate for the currently selected video settings. */
@Composable
private fun rememberEstimateMb(
    settings: EditorViewModel.ExportSettings,
    currentAspectRatio: AspectRatioType,
    projectLengthMs: Long
): Double {
    val (w, h, mb) = androidx.compose.runtime.remember(
        currentAspectRatio, settings.resolution, settings.fps, settings.bitrateMbps, projectLengthMs
    ) {
        val (width, height) = CanvasMath.canvasSize(currentAspectRatio, settings.resolution)
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

/**
 * CapCut export selectors are horizontal tracks with a dot and label per stop.
 */
@Composable
internal fun ExportSelectorTrack(
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
