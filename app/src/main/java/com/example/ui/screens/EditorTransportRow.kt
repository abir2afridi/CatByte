package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.CapCutIconFullscreen
import com.example.ui.components.CapCutIconMagnetOn
import com.example.ui.components.CapCutIconPlayTriangle
import java.util.Locale

/**
 * CapCut transport row:
 * - Left: Fullscreen preview button, Aspect ratio chip & Live Timecode (00:04.12 / 00:15.00)
 * - Center: Split Scissors button + Play/Pause button
 * - Right: Magnetic snap toggle, Undo, Redo
 */
@Composable
internal fun EditorTransportRow(
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    magneticSnap: Boolean = true,
    aspectRatioLabel: String = "16:9",
    currentPositionMs: Long = 0L,
    totalDurationMs: Long = 0L,
    onAspectRatioClick: (() -> Unit)? = null,
    onTogglePlay: () -> Unit,
    onSplit: (() -> Unit)? = null,
    onStepBackward: (() -> Unit)? = null,
    onStepForward: (() -> Unit)? = null,
    onFullscreen: () -> Unit,
    onToggleMagneticSnap: () -> Unit = {},
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTransition: (() -> Unit)? = null,
    onSeek: ((Long) -> Unit)? = null
) {
    val barBackground = Color(0xFF101014)
    var showJumpTimecodeDialog by remember { mutableStateOf(false) }
    var jumpInputSeconds by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(barBackground)
            .testTag("editor_transport_row")
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fullscreen preview button
            IconButton(
                onClick = onFullscreen,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("editor_fullscreen_button")
            ) {
                CapCutIconFullscreen(
                    tint = Color.White,
                    size = 18.dp
                )
            }

            // Canvas aspect ratio chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF2A2A31))
                    .then(
                        if (onAspectRatioClick != null) {
                            Modifier.clickable(onClick = onAspectRatioClick)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("editor_aspect_ratio_chip"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = aspectRatioLabel,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive Timecode Display (Toggle Timecode / Frame Count & Tap for Jump Dialog)
            var showFramesMode by remember { mutableStateOf(false) }
            val timecodeDisplay = if (showFramesMode) {
                val curF = (currentPositionMs / 33.33f).toInt()
                val totF = (totalDurationMs / 33.33f).toInt()
                "F $curF / $totF"
            } else {
                "${formatTimecode(currentPositionMs)} / ${formatTimecode(totalDurationMs)}"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E1F24))
                    .clickable {
                        if (onSeek != null) {
                            jumpInputSeconds = String.format(Locale.US, "%.2f", currentPositionMs / 1000f)
                            showJumpTimecodeDialog = true
                        } else {
                            showFramesMode = !showFramesMode
                        }
                    }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = timecodeDisplay,
                    color = if (showFramesMode) Color(0xFFFFD600) else Color(0xFF26E5D7),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: Split button + Play/Pause button
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSplit != null) {
                IconButton(
                    onClick = onSplit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("editor_quick_split_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Split clip",
                        tint = Color(0xFF26E5D7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onStepBackward != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E1F24))
                        .clickable(onClick = onStepBackward),
                    contentAlignment = Alignment.Center
                ) {
                    Text("-1F", color = Color(0xFFA5A8B2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onTogglePlay() }
                    .testTag("editor_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    CapCutIconPlayTriangle(
                        tint = Color.White,
                        size = 22.dp
                    )
                }
            }

            if (onStepForward != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E1F24))
                        .clickable(onClick = onStepForward),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+1F", color = Color(0xFFA5A8B2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Right: Magnetic Snap [ON], Undo, Redo
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Magnetic Snapping / Track Magnet toggle
            IconButton(
                onClick = onToggleMagneticSnap,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("editor_magnetic_snap_button")
            ) {
                CapCutIconMagnetOn(
                    isOn = magneticSnap,
                    tint = Color.White,
                    size = 22.dp
                )
            }

            // Undo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("action_undo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color.Gray.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Redo
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("action_redo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color.Gray.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        if (showJumpTimecodeDialog) {
            AlertDialog(
                onDismissRequest = { showJumpTimecodeDialog = false },
                title = {
                    Text("Jump to Timecode", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Enter timestamp in seconds (Total: ${String.format(Locale.US, "%.1f", totalDurationMs / 1000f)}s):",
                            color = Color(0xFFA5A8B2),
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = jumpInputSeconds,
                            onValueChange = { jumpInputSeconds = it },
                            singleLine = true,
                            placeholder = { Text("e.g. 5.5", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF26E5D7),
                                unfocusedBorderColor = Color(0xFF33353E)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    onSeek?.invoke(0L)
                                    showJumpTimecodeDialog = false
                                }
                            ) {
                                Text("⏮ Start", color = Color(0xFF26E5D7), fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = {
                                    onSeek?.invoke(totalDurationMs / 2L)
                                    showJumpTimecodeDialog = false
                                }
                            ) {
                                Text("⏸ Middle", color = Color(0xFF26E5D7), fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = {
                                    onSeek?.invoke(totalDurationMs)
                                    showJumpTimecodeDialog = false
                                }
                            ) {
                                Text("⏭ End", color = Color(0xFF26E5D7), fontSize = 11.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val secs = jumpInputSeconds.toDoubleOrNull()
                            if (secs != null) {
                                val targetMs = (secs * 1000).toLong().coerceIn(0L, totalDurationMs)
                                onSeek?.invoke(targetMs)
                            }
                            showJumpTimecodeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7))
                    ) {
                        Text("Jump", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpTimecodeDialog = false }) {
                        Text("Cancel", color = Color(0xFFA5A8B2))
                    }
                },
                containerColor = Color(0xFF1E1F24)
            )
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 10
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)
}

