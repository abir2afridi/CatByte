package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import com.example.model.TimelineMarker
import com.example.ui.components.CapCutIconFullscreen
import java.util.Locale

/**
 * The strip below the transport bar.
 *
 * The fullscreen button, the aspect ratio chip and the live timecode used to
 * sit inside the transport row itself, where they overlapped the split button
 * on a narrow phone (the timecode ran into the scissors icon). They now live
 * in their own row, so the transport row only carries the playback controls.
 *
 * The row also carries the marker jump buttons: they are free of space
 * pressure here, and a marker is the fastest way to reach a cut.
 */
@Composable
internal fun EditorTransportInfoRow(
    aspectRatioLabel: String,
    currentPositionMs: Long,
    totalDurationMs: Long,
    markers: List<TimelineMarker> = emptyList(),
    /** Safe-area grid toggle chip (moved here from the preview canvas). */
    showSafeAreaGuides: Boolean = false,
    onToggleGrid: () -> Unit = {},
    onAspectRatioClick: (() -> Unit)? = null,
    onFullscreen: () -> Unit,
    onSeek: (Long) -> Unit,
    onJumpToMarker: (String) -> Unit,
    /** Jumps to the marker before the playhead (handled by the view model). */
    onPreviousMarker: () -> Unit = {},
    /** Jumps to the marker after the playhead (handled by the view model). */
    onNextMarker: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF101014))
            .padding(horizontal = 6.dp)
            .testTag("editor_transport_info_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fullscreen preview button
        IconButton(
            onClick = onFullscreen,
            modifier = Modifier
                .size(30.dp)
                .testTag("editor_fullscreen_button")
        ) {
            CapCutIconFullscreen(
                tint = Color.White,
                size = 16.dp
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
                .padding(horizontal = 7.dp, vertical = 3.dp)
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

        // [POSITION-LOCKED — user approved 2026-11-21]
        // Grid / safe-area toggle sits HERE, next to the 1:1 ratio chip (it
        // used to float on the preview canvas - the user moved it down here).
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (showSafeAreaGuides) Color(0xFF26E5D7).copy(alpha = 0.2f) else Color(0xFF2A2A31)
                )
                .border(
                    width = 0.8.dp,
                    color = if (showSafeAreaGuides) Color(0xFF26E5D7) else Color(0xFF33353E),
                    shape = RoundedCornerShape(5.dp)
                )
                .clickable(onClick = onToggleGrid)
                .padding(horizontal = 7.dp, vertical = 3.dp)
                .testTag("editor_grid_chip")
        ) {
            Text(
                text = if (showSafeAreaGuides) "Grid ON" else "Grid",
                color = if (showSafeAreaGuides) Color(0xFF26E5D7) else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Marker navigation: jump to the marker before / after the playhead.
        // The view model owns the "which marker" decision, the row only asks.
        if (markers.isNotEmpty()) {
            IconButton(
                onClick = onPreviousMarker,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("editor_previous_marker_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous marker",
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onNextMarker,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("editor_next_marker_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next marker",
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

}

/**
 * The "Jump to Timecode" dialog: a seconds field, the usual shortcuts and the
 * project markers, so a marker is reachable without opening another sheet.
 */
@Composable
internal fun JumpToTimecodeDialog(
    currentPositionMs: Long,
    totalDurationMs: Long,
    markers: List<TimelineMarker>,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    onJumpToMarker: (String) -> Unit
) {
    var jumpInputSeconds by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    TextButton(onClick = { onSeek(0L) }) {
                        Text("⏮ Start", color = Color(0xFF26E5D7), fontSize = 11.sp)
                    }
                    TextButton(onClick = { onSeek(totalDurationMs / 2L) }) {
                        Text("⏸ Middle", color = Color(0xFF26E5D7), fontSize = 11.sp)
                    }
                    TextButton(onClick = { onSeek(totalDurationMs) }) {
                        Text("⏭ End", color = Color(0xFF26E5D7), fontSize = 11.sp)
                    }
                }

                if (markers.isNotEmpty()) {
                    Text(
                        "Markers",
                        color = Color(0xFFFFD600),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        markers.sortedBy { it.atMs }.forEach { marker ->
                            MarkerJumpRow(marker = marker, onClick = { onJumpToMarker(marker.id) })
                        }
                    }
                }

                Text(
                    "Frames: F ${currentPositionMs / 33.33f} / ${totalDurationMs / 33.33f}",
                    color = Color(0xFF6E6E73),
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val secs = jumpInputSeconds.toDoubleOrNull()
                    if (secs != null) {
                        onSeek((secs * 1000).toLong().coerceIn(0L, totalDurationMs))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26E5D7))
            ) {
                Text("Jump", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA5A8B2))
            }
        },
        containerColor = Color(0xFF1E1F24)
    )
}

/** One marker inside the jump dialog. */
@Composable
private fun MarkerJumpRow(
    marker: TimelineMarker,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF26272E))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("editor_jump_marker_${marker.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = marker.label.ifBlank { "Marker" },
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1
        )
        Text(
            text = formatTimecodeTenths(marker.atMs),
            color = Color(0xFF8E8E93),
            fontSize = 11.sp
        )
    }
}
