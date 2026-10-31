package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SampleDataProvider
import com.example.model.AspectRatioType
import com.example.model.AudioClip
import com.example.model.BlendMode
import com.example.model.ClipAnimation
import com.example.model.EasingCurve
import com.example.model.EffectType
import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.Keyframe
import com.example.model.KeyframeProperty
import com.example.model.MaskData
import com.example.model.MaskType
import com.example.model.MediaClip
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import com.example.model.TransitionType
import com.example.model.VideoFilter
import com.example.ui.theme.CapCutBorder
import com.example.ui.theme.CapCutBorderSubtle
import com.example.ui.theme.CapCutCyan
import com.example.ui.theme.CapCutDarkBackground
import com.example.ui.theme.CapCutGreen
import com.example.ui.theme.CapCutPink
import com.example.ui.theme.CapCutPurple
import com.example.ui.theme.CapCutRed
import com.example.ui.theme.CapCutSurface
import com.example.ui.theme.CapCutSurfaceElevated
import com.example.ui.theme.CapCutSurfaceVariant
import com.example.ui.theme.CapCutTextPrimary
import com.example.ui.theme.CapCutTextSecondary
import com.example.ui.theme.CapCutYellow
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip
import com.example.ui.theme.TornPaperEdge
import com.example.ui.theme.WashiTapeCorner
import com.example.ui.theme.WashiTapeSticker
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder
import com.example.viewmodel.ExportUiState

// ==========================================
// 1. Edit Clip Sheet (Split, Trim, Delete, etc.)
// ==========================================

@Composable
fun EditClipSheet(
    clip: MediaClip?,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRotate: () -> Unit,
    onMirror: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenAdjust: () -> Unit,
    onOpenKeyframe: () -> Unit = {},
    onOpenBlendMode: () -> Unit = {},
    onOpenMask: () -> Unit = {},
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(CapCutSurface)
            .border(1.dp, CapCutBorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp)
            .testTag("edit_clip_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Clip: ${clip?.title ?: "Select a clip"}",
                color = CapCutTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                icon = Icons.Default.ContentCut,
                label = "Split",
                accentColor = CapCutCyan,
                testTag = "action_split_button",
                onClick = onSplit
            )
            ToolButton(
                icon = Icons.Default.AutoAwesome,
                label = "Keyframe",
                accentColor = CapCutPink,
                testTag = "action_keyframe_button",
                onClick = onOpenKeyframe
            )
            ToolButton(
                icon = Icons.Default.Tune,
                label = "Blend",
                accentColor = CapCutPurple,
                testTag = "action_blend_button",
                onClick = onOpenBlendMode
            )
            ToolButton(
                icon = Icons.Default.Speed,
                label = "Mask",
                accentColor = CapCutGreen,
                testTag = "action_mask_button",
                onClick = onOpenMask
            )
            ToolButton(
                icon = Icons.Default.Speed,
                label = "Speed",
                accentColor = Color.Yellow,
                testTag = "action_speed_button",
                onClick = onOpenSpeed
            )
            ToolButton(
                icon = Icons.Default.Tune,
                label = "Adjust",
                accentColor = CapCutGreen,
                testTag = "action_adjust_button",
                onClick = onOpenAdjust
            )
            ToolButton(
                icon = Icons.Default.RotateRight,
                label = "Rotate",
                accentColor = Color.White,
                testTag = "action_rotate_button",
                onClick = onRotate
            )
            ToolButton(
                icon = Icons.Default.ContentCopy,
                label = "Duplicate",
                accentColor = Color.White,
                testTag = "action_duplicate_button",
                onClick = onDuplicate
            )
            ToolButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                accentColor = CapCutRed,
                testTag = "action_delete_button",
                onClick = onDelete
            )
        }
    }
}

// ==========================================
// 2. Speed Sheet (Normal & Velocity Curves)
// ==========================================

@Composable
fun SpeedSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onSelectSpeedCurve: ((com.example.model.SpeedCurve) -> Unit)? = null,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var speedVal by remember { mutableFloatStateOf(currentSpeed) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("speed_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚡", fontSize = 16.sp)
                    Text("Speed & Velocity Curves", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${String.format("%.1f", speedVal)}x",
                color = colors.accentCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Slider(
                value = speedVal,
                onValueChange = {
                    speedVal = it
                    onSpeedSelected(it)
                },
                valueRange = 0.2f..5.0f,
                steps = 47,
                colors = SliderDefaults.colors(
                    thumbColor = colors.border,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Speed Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { s ->
                    val isSelected = kotlin.math.abs(speedVal - s) < 0.05f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.accentCyan.copy(alpha = 0.25f) else colors.surfaceElevated)
                            .pencilHatching(
                                color = if (isSelected) colors.accentCyan.copy(alpha = 0.15f) else Color.Transparent,
                                spacing = 5.dp
                            )
                            .sketchPencilBorder(
                                strokeWidth = if (isSelected) 1.6.dp else 1.dp,
                                color = if (isSelected) colors.accentCyan else colors.borderSubtle,
                                cornerRadius = 6.dp
                            )
                            .clickable {
                                speedVal = s
                                onSpeedSelected(s)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${s}x",
                            color = if (isSelected) colors.accentCyan else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Velocity Curves (Beat Sync Presets)", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.model.SpeedCurvePreset.entries.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .sketchPencilBorder(
                                strokeWidth = 1.2.dp,
                                color = colors.borderSubtle,
                                cornerRadius = 8.dp
                            )
                            .clickable {
                                val curve = com.example.data.SpeedRampEngine.getPresetCurve(preset)
                                onSelectSpeedCurve?.invoke(curve)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(preset.displayName, color = colors.accentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ==========================================
// Custom Curve Editor Sheet
// ==========================================

@Composable
fun SpeedCurveEditorSheet(
    speedCurve: SpeedCurve,
    onCurveChanged: (SpeedCurve) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(600.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("speed_curve_editor_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Speed Curve", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            SpeedCurveEditor(
                speedCurve = speedCurve,
                onCurveChanged = onCurveChanged,
                onClose = onClose
            )
        }
    }
}

// ==========================================
// 3. Audio & Voice Recorder Sheet
// ===========================================
@Composable
fun AudioSheet(
    isRecordingVoice: Boolean,
    voiceDurationMs: Long,
    onAddAudio: (AudioClip) -> Unit,
    onExtractAudio: (() -> Unit)? = null,
    onStartVoiceRecord: () -> Unit,
    onStopVoiceRecord: () -> Unit,
    onTtsVoiceover: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("✏️ Foley", "🎵 Music", "🔔 SFX", "🎙️ Mic")

    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("audio_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎵", fontSize = 16.sp)
                    Text("Audio & Foley Studio", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    if (onExtractAudio != null) {
                        Button(
                            onClick = { onExtractAudio.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentPurple),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Extract Audio 🔊", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }
            }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CapCutSurface,
            contentColor = CapCutPurple,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CapCutPurple
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Sketchbook Foley ASMR Library
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SampleDataProvider.sketchbookFoleyList.forEach { foley ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CapCutSurfaceElevated)
                                .border(1.dp, CapCutBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(foley.title, color = CapCutTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${foley.artist} • ${foley.durationMs / 1000f}s", color = CapCutTextSecondary, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onAddAudio(foley) },
                                colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Add ✏️", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            1 -> {
                // Music Library
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SampleDataProvider.trendingAudioList.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CapCutSurfaceElevated)
                                .border(1.dp, CapCutBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = CapCutTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${track.artist} • ${track.durationMs / 1000}s", color = CapCutTextSecondary, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onAddAudio(track) },
                                colors = ButtonDefaults.buttonColors(containerColor = CapCutPurple),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Use", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            2 -> {
                // Sound Effects
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SampleDataProvider.soundEffectsList.forEach { sfx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CapCutSurfaceElevated)
                                .border(1.dp, CapCutBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sfx.title, color = CapCutTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${sfx.durationMs / 1000f}s", color = CapCutCyan, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onAddAudio(sfx) },
                                colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Add", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            3 -> {
                // Voice Recorder
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val sec = voiceDurationMs / 1000f
                    Text(
                        text = if (isRecordingVoice) String.format("Recording: %02d:%04.1f", (sec / 60).toInt(), sec % 60) else "Hold or Tap to Record Voiceover",
                        color = if (isRecordingVoice) CapCutRed else Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val pulseScale: androidx.compose.runtime.MutableState<Float> = remember { mutableStateOf(1f) }
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(isRecordingVoice) {
                        if (isRecordingVoice) {
                            scope.launch {
                                var growing = true
                                while (isRecordingVoice) {
                                    val target = if (growing) 1.25f else 1.0f
                                    growing = !growing
                                    // Simple linear interpolation over 600ms
                                    val start = pulseScale.value
                                    val duration = 600L
                                    val startTime = System.currentTimeMillis()
                                    while (System.currentTimeMillis() - startTime < duration) {
                                        if (!isRecordingVoice) break
                                        val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
                                        pulseScale.value = start + (target - start) * progress
                                        delay(16) // ~60fps
                                    }
                                    if (!isRecordingVoice) break
                                    pulseScale.value = target
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(if (isRecordingVoice) pulseScale.value else 1.0f)
                            .clip(CircleShape)
                            .background(if (isRecordingVoice) CapCutRed else CapCutSurfaceElevated)
                            .border(2.dp, if (isRecordingVoice) Color.White else CapCutBorder, CircleShape)
                            .clickable {
                                if (isRecordingVoice) onStopVoiceRecord() else onStartVoiceRecord()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice Record",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // TTS Voiceover Button
                    if (onTtsVoiceover != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onTtsVoiceover() },
                            colors = ButtonDefaults.buttonColors(containerColor = CapCutPurple),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "TTS", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Voiceover", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isRecordingVoice) "Tap to finish and add to timeline" else "Speak clearly into your phone's microphone",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
}

// ==========================================
// 4. Text Editor Sheet
// ==========================================

@Composable
fun TextEditorSheet(
    textOverlay: TextOverlay?,
    onAddOrUpdateText: (text: String, fontStyle: TextFontStyle, color: Long, anim: TextAnimationType) -> Unit,
    onDeleteText: () -> Unit,
    onAutoCaptions: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    var textInput by remember { mutableStateOf(textOverlay?.text ?: "Viral TikTok Text 🔥") }
    var fontStyle by remember { mutableStateOf(textOverlay?.fontStyle ?: TextFontStyle.CAPCUT_BOLD) }
    var selectedColor by remember { mutableLongStateOf(textOverlay?.color ?: 0xFFFFFFFF) }
    var animation by remember { mutableStateOf(textOverlay?.animation ?: TextAnimationType.POP_UP) }

    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("text_editor_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add & Style Text", color = CapCutTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onAutoCaptions != null) {
                        Button(
                            onClick = { onAutoCaptions.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentYellow),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Auto Captions 🤖", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (textOverlay != null) {
                        IconButton(onClick = onDeleteText) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CapCutRed)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
                    }
                }
            }

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Enter text...", color = CapCutTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CapCutCyan,
                unfocusedBorderColor = CapCutBorder,
                focusedTextColor = CapCutTextPrimary,
                unfocusedTextColor = CapCutTextPrimary,
                focusedContainerColor = CapCutSurfaceElevated,
                unfocusedContainerColor = CapCutSurfaceElevated
            )
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text("Font Style", color = CapCutTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(TextFontStyle.values()) { style ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (fontStyle == style) CapCutCyan else CapCutSurfaceElevated)
                        .clickable { fontStyle = style }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = style.displayName,
                        color = if (fontStyle == style) Color.Black else CapCutTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Color", color = CapCutTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            listOf(0xFFFFFFFF, 0xFFFFD166, 0xFF00E5FF, 0xFFFF3358, 0xFFFF006E, 0xFF06D6A0, 0xFF000000).forEach { colorLong ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(
                            width = if (selectedColor == colorLong) 2.dp else 1.dp,
                            color = if (selectedColor == colorLong) CapCutCyan else CapCutBorder,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = colorLong }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Animation", color = CapCutTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(TextAnimationType.values()) { anim ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (animation == anim) CapCutYellow else CapCutSurfaceElevated)
                        .clickable { animation = anim }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = anim.displayName,
                        color = if (animation == anim) Color.Black else CapCutTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                onAddOrUpdateText(textInput, fontStyle, selectedColor, animation)
                onClose()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply to Timeline", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
}

// ==========================================
// 5. Effects Sheet
// ==========================================

@Composable
fun EffectsSheet(
    onAddEffect: (EffectType) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(CapCutSurface)
            .border(1.dp, CapCutBorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp)
            .testTag("effects_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Video Effects (CapCut FX)", color = CapCutTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = CapCutTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EffectType.values().forEach { fx ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CapCutSurfaceElevated)
                        .border(1.dp, CapCutBorder, RoundedCornerShape(8.dp))
                        .clickable { onAddEffect(fx) }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CapCutGreen.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = fx.displayName,
                            tint = CapCutGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = fx.displayName,
                        color = CapCutTextPrimary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. Filters Sheet
// ==========================================

@Composable
fun FiltersSheet(
    activeFilter: VideoFilter,
    onSelectFilter: (VideoFilter) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("filters_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎨", fontSize = 16.sp)
                    Text("Sketch & Color Filters", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VideoFilter.values().forEach { filter ->
                    val isSelected = filter == activeFilter
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(86.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accentCyan.copy(alpha = 0.22f) else colors.surfaceElevated)
                            .pencilHatching(
                                color = if (isSelected) colors.accentCyan.copy(alpha = 0.12f) else Color.Transparent,
                                spacing = 5.dp
                            )
                            .sketchPencilBorder(
                                strokeWidth = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) colors.accentCyan else colors.borderSubtle,
                                cornerRadius = 8.dp
                            )
                            .clickable { onSelectFilter(filter) }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        when (filter) {
                                            VideoFilter.SKETCH_GRAPHITE -> listOf(Color(0xFF2C2B28), Color(0xFFD6CEBD))
                                            VideoFilter.BLUEPRINT -> listOf(Color(0xFF0F172A), Color(0xFF38BDF8))
                                            VideoFilter.SEPIA_NOTEBOOK -> listOf(Color(0xFF78350F), Color(0xFFFDE68A))
                                            VideoFilter.CHARCOAL_SMUDGE -> listOf(Color.Black, Color(0xFF52525B))
                                            VideoFilter.HALFTONE_MANGA -> listOf(Color(0xFF18181B), Color(0xFFE4E4E7))
                                            VideoFilter.CYBERPUNK -> listOf(Color.Cyan, Color.Magenta)
                                            VideoFilter.GOLDEN_HOUR -> listOf(Color.Yellow, Color(0xFFFF5722))
                                            VideoFilter.VINTAGE -> listOf(Color(0xFF8D6E63), Color(0xFFFFE082))
                                            VideoFilter.NOIR -> listOf(Color.Black, Color.White)
                                            VideoFilter.TEAL_ORANGE -> listOf(Color(0xFF00B4D8), Color(0xFFFF6B6B))
                                            VideoFilter.GLITCH_ART -> listOf(Color.Red, Color.Blue)
                                            else -> listOf(Color.DarkGray, Color.LightGray)
                                        }
                                    )
                                )
                                .border(1.dp, colors.border, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = filter.displayName,
                            color = if (isSelected) colors.accentCyan else colors.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. Adjustments Sheet
// ==========================================

@Composable
fun AdjustmentsSheet(
    clip: MediaClip?,
    onUpdateAdjustments: (brightness: Float, contrast: Float, saturation: Float, vignette: Float) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var brightness by remember { mutableFloatStateOf(clip?.brightness ?: 0f) }
    var contrast by remember { mutableFloatStateOf(clip?.contrast ?: 1f) }
    var saturation by remember { mutableFloatStateOf(clip?.saturation ?: 1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("adjustments_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎚️", fontSize = 16.sp)
                    Text("Adjust Image & Lead Shading", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Brightness (Paper Exposure)", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("${String.format("%.2f", brightness)}", color = colors.accentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    onUpdateAdjustments(brightness, contrast, saturation, 0f)
                },
                valueRange = -0.5f..0.5f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.border,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Contrast (Graphite Intensity)", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("${String.format("%.2f", contrast)}", color = colors.accentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = contrast,
                onValueChange = {
                    contrast = it
                    onUpdateAdjustments(brightness, contrast, saturation, 0f)
                },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.border,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saturation (Color Pencil Vibrance)", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("${String.format("%.2f", saturation)}", color = colors.accentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    onUpdateAdjustments(brightness, contrast, saturation, 0f)
                },
                valueRange = 0.0f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.border,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )
        }
    }
}


// ==========================================
// 8. Canvas & Aspect Ratio Sheet
// ==========================================

@Composable
fun CanvasRatioSheet(
    activeRatio: AspectRatioType,
    currentPaperStyle: String = "Drafting Grid",
    onSelectRatio: (AspectRatioType) -> Unit,
    onSelectPaperStyle: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val paperStyles = listOf(
        "Drafting Grid",
        "Parchment Sketchbook",
        "Architect Blueprint",
        "Heavy Kraft Paper",
        "Dark Slate Chalkboard",
        "French Ruled Graph"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("canvas_ratio_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📐", fontSize = 16.sp)
                    Text("Canvas & Paper Blueprint", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Aspect Ratio", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AspectRatioType.values().forEach { r ->
                    val isSelected = r == activeRatio
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accentCyan.copy(alpha = 0.22f) else colors.surfaceElevated)
                            .pencilHatching(
                                color = if (isSelected) colors.accentCyan.copy(alpha = 0.15f) else Color.Transparent,
                                spacing = 5.dp
                            )
                            .sketchPencilBorder(
                                strokeWidth = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) colors.accentCyan else colors.borderSubtle,
                                cornerRadius = 8.dp
                            )
                            .clickable { onSelectRatio(r) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = r.label,
                            color = if (isSelected) colors.accentCyan else colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (r) {
                                AspectRatioType.RATIO_9_16 -> "Shorts/Reels"
                                AspectRatioType.RATIO_16_9 -> "YouTube"
                                AspectRatioType.RATIO_1_1 -> "Square"
                                AspectRatioType.RATIO_4_5 -> "Feed Post"
                            },
                            color = colors.textSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Sketch Paper Texture", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paperStyles.forEach { style ->
                    val isSelected = style.equals(currentPaperStyle, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accentYellow.copy(alpha = 0.2f) else colors.surfaceElevated)
                            .pencilHatching(
                                color = if (isSelected) colors.accentYellow.copy(alpha = 0.12f) else Color.Transparent,
                                spacing = 6.dp
                            )
                            .sketchPencilBorder(
                                strokeWidth = if (isSelected) 1.6.dp else 1.dp,
                                color = if (isSelected) colors.accentYellow else colors.borderSubtle,
                                cornerRadius = 8.dp
                            )
                            .clickable { onSelectPaperStyle(style) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "📜 $style",
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


// ==========================================
// 9. Stickers Sheet
// ==========================================

@Composable
fun StickersSheet(
    onAddSticker: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val allStickers = SampleDataProvider.doodleStickers
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("stickers_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✏️", fontSize = 16.sp)
                    Text("Handcrafted Doodles & Stickers", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-row scrollable doodle sticker palette
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    allStickers.take(allStickers.size / 2).forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceElevated)
                                .sketchPencilBorder(strokeWidth = 1.2.dp, color = colors.border, cornerRadius = 10.dp)
                                .clickable { onAddSticker(emoji) }
                                .testTag("sticker_$emoji"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                // Row 2
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    allStickers.drop(allStickers.size / 2).forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceElevated)
                                .sketchPencilBorder(strokeWidth = 1.2.dp, color = colors.border, cornerRadius = 10.dp)
                                .clickable { onAddSticker(emoji) }
                                .testTag("sticker_$emoji"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. Transitions Picker Sheet
// ==========================================

@Composable
fun TransitionsPickerSheet(
    clipId: String,
    onSelectTransition: (TransitionType) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("transitions_sheet")
    ) {
        TornPaperEdge(
            paperColor = colors.surfaceElevated,
            borderColor = colors.borderSubtle
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎬", fontSize = 16.sp)
                    Text("Paper & Sketch Transitions", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransitionType.values().forEach { trans ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .sketchPencilBorder(strokeWidth = 1.2.dp, color = colors.borderSubtle, cornerRadius = 8.dp)
                            .clickable {
                                onSelectTransition(trans)
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = trans.displayName,
                            tint = colors.accentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(trans.displayName, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


// ==========================================
// 11. Export Dialog
// ==========================================

@Composable
fun ExportDialog(
    exportState: ExportUiState,
    onStartExport: (resolution: String, fps: Int) -> Unit,
    onStartBackgroundExport: ((Context, String, Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val colors = PencilTheme.colors
    var selectedRes by remember { mutableStateOf("1080P") }
    var selectedFps by remember { mutableIntStateOf(30) }
    val currentContext = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CapCutSurface)
                .sketchPencilBorder(strokeWidth = 2.dp, color = CapCutBorder, cornerRadius = 18.dp)
                .padding(20.dp)
                .testTag("export_dialog")
        ) {
            if (exportState.isExporting) {
                // Rendering Progress Mode with Sharpened Pencil Animation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharpenedPencilTip(
                            modifier = Modifier.size(24.dp),
                            leadColor = CapCutBorder,
                            woodColor = CapCutYellow
                        )
                        Text("Drafting & Exporting Video...", color = CapCutTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportState.stage, color = CapCutCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(18.dp))
                    LinearProgressIndicator(
                        progress = { exportState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = CapCutCyan,
                        trackColor = CapCutSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${(exportState.progress * 100).toInt()}%",
                        color = CapCutTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Rendering frames with precision pencil lead", color = CapCutTextSecondary, fontSize = 11.sp)
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
                            .background(CapCutGreen.copy(alpha = 0.2f))
                            .border(1.5.dp, CapCutGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = CapCutGreen, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Masterpiece Ready!", color = CapCutTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Rendered in ${exportState.resolution} @ ${exportState.fps}fps", color = CapCutTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("TikTok" to CapCutCyan, "Instagram" to CapCutPink, "WhatsApp" to CapCutGreen).forEach { (platform, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CapCutSurfaceElevated)
                                    .border(1.dp, CapCutBorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { /* Share */ }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = platform, tint = color, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(platform, color = CapCutTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CapCutCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Resolution & FPS Picker Mode
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📐", fontSize = 18.sp)
                        Text("Export Blueprint", color = CapCutTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Resolution", color = CapCutTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                    .background(if (isSelected) CapCutCyan else CapCutSurfaceElevated)
                                    .border(1.dp, if (isSelected) CapCutCyan else CapCutBorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { selectedRes = res }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res,
                                    color = if (isSelected) Color.Black else CapCutTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Frame Rate", color = CapCutTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                    .background(if (isSelected) CapCutCyan else CapCutSurfaceElevated)
                                    .border(1.dp, if (isSelected) CapCutCyan else CapCutBorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${fps}fps",
                                    color = if (isSelected) Color.Black else CapCutTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    var backgroundExport by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = backgroundExport,
                            onCheckedChange = { backgroundExport = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = CapCutCyan,
                                uncheckedColor = CapCutTextSecondary
                            )
                        )
                        Text(
                            "Background export (continues if app closes)",
                            color = CapCutTextPrimary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CapCutSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = CapCutTextPrimary)
                        }
                        Button(
                            onClick = {
                                if (backgroundExport) {
                                    onStartBackgroundExport?.invoke(currentContext, selectedRes, selectedFps)
                                } else {
                                    onStartExport(selectedRes, selectedFps)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CapCutBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (backgroundExport) "Export in Background 🌙" else "Export 🚀",
                                color = if (colors.isLight) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Utility Tool Button
@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .pencilHatching(color = accentColor.copy(alpha = 0.08f), spacing = 5.dp)
                .sketchPencilBorder(strokeWidth = 1.3.dp, color = colors.border, cornerRadius = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Phase 1: Blend Mode Picker Sheet
@Composable
fun BlendModeSheet(
    selectedMode: BlendMode,
    onSelectBlendMode: (BlendMode) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("blend_mode_sheet")
    ) {
        TornPaperEdge(paperColor = colors.surfaceElevated, borderColor = colors.borderSubtle)
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎨", fontSize = 16.sp)
                    Text("PIP Blending Modes", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(BlendMode.entries.toTypedArray()) { mode ->
                    val isSel = mode == selectedMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) colors.accentCyan else colors.surfaceElevated)
                            .sketchPencilBorder(strokeWidth = 1.2.dp, color = if (isSel) colors.border else colors.borderSubtle, cornerRadius = 10.dp)
                            .clickable { onSelectBlendMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSel) Color.White else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Phase 1: Masking Sheet
@Composable
fun MaskSheet(
    maskData: MaskData,
    onUpdateMask: (MaskData) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var currentMask by remember(maskData) { mutableStateOf(maskData) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("mask_sheet")
    ) {
        TornPaperEdge(paperColor = colors.surfaceElevated, borderColor = colors.borderSubtle)
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎭", fontSize = 16.sp)
                    Text("Video Masking", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MaskType.entries.toTypedArray()) { type ->
                    val isSel = type == currentMask.type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colors.accentPurple else colors.surfaceElevated)
                            .sketchPencilBorder(strokeWidth = 1.2.dp, color = if (isSel) colors.border else colors.borderSubtle, cornerRadius = 8.dp)
                            .clickable {
                                currentMask = currentMask.copy(type = type)
                                onUpdateMask(currentMask)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            color = if (isSel) Color.White else colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (currentMask.type != MaskType.NONE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mask Size / Scale", color = colors.textSecondary, fontSize = 11.sp)
                    Text("${(currentMask.width * 100).toInt()}%", color = colors.accentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = currentMask.width,
                    onValueChange = {
                        currentMask = currentMask.copy(width = it, height = it)
                        onUpdateMask(currentMask)
                    },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = colors.border, activeTrackColor = colors.accentPurple)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Feather (Edge Softness)", color = colors.textSecondary, fontSize = 11.sp)
                    Text("${(currentMask.feather * 100).toInt()}%", color = colors.accentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = currentMask.feather,
                    onValueChange = {
                        currentMask = currentMask.copy(feather = it)
                        onUpdateMask(currentMask)
                    },
                    valueRange = 0.0f..0.5f,
                    colors = SliderDefaults.colors(thumbColor = colors.border, activeTrackColor = colors.accentPurple)
                )
            }
        }
    }
}

// Phase 1: Keyframe Animation Manager Sheet
@Composable
fun KeyframeSheet(
    clip: MediaClip?,
    onAddKeyframe: (property: KeyframeProperty, value: Float, easing: EasingCurve) -> Unit,
    onRemoveKeyframe: (keyframeId: String) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var selectedProperty by remember { mutableStateOf(KeyframeProperty.SCALE) }
    var selectedEasing by remember { mutableStateOf(EasingCurve.EASE_IN_OUT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.surface)
            .sketchPencilBorder(strokeWidth = 1.6.dp, color = colors.border, cornerRadius = 18.dp)
            .testTag("keyframe_sheet")
    ) {
        TornPaperEdge(paperColor = colors.surfaceElevated, borderColor = colors.borderSubtle)
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💎", fontSize = 16.sp)
                    Text("Keyframe Animator", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Select Animated Property:", color = colors.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(KeyframeProperty.entries.toTypedArray()) { prop ->
                    val isSel = prop == selectedProperty
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colors.accentPink else colors.surfaceElevated)
                            .clickable { selectedProperty = prop }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(prop.displayName, color = if (isSel) Color.White else colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val initialValue = when (selectedProperty) {
                            KeyframeProperty.SCALE -> clip?.scale ?: 1f
                            KeyframeProperty.POSITION_X -> clip?.posX ?: 0.5f
                            KeyframeProperty.POSITION_Y -> clip?.posY ?: 0.5f
                            KeyframeProperty.ROTATION -> clip?.rotation ?: 0f
                            KeyframeProperty.OPACITY -> clip?.opacity ?: 1f
                            KeyframeProperty.VOLUME -> clip?.volume ?: 1f
                        }
                        onAddKeyframe(selectedProperty, initialValue, selectedEasing)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentPink),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add Keyframe 💎", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text("${clip?.keyframes?.size ?: 0} Active Keyframes", color = colors.textSecondary, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            val clipKeyframes = clip?.keyframes ?: emptyList()
            if (clipKeyframes.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clipKeyframes) { kf ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💎 ${kf.property.displayName} @ ${kf.timestampMs}ms", color = colors.textPrimary, fontSize = 10.sp)
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = colors.accentRed,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onRemoveKeyframe(kf.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

