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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CapCutIconMagnetOn
import com.example.ui.components.CapCutIconPlayTriangle

/**
 * CapCut transport row:
 * - Center: Split Scissors button, frame step buttons + Play/Pause button
 * - Right: Magnetic snap toggle, Undo, Redo
 *
 * The fullscreen button, the aspect ratio chip and the live timecode live in
 * [EditorTransportInfoRow] below this bar: inside the bar they overlapped the
 * split button on a narrow phone.
 */
@Composable
internal fun EditorTransportRow(
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    magneticSnap: Boolean = true,
    /** Live timecode chip on the left (opens the jump dialog). */
    timecodeText: String? = null,
    onTimecodeClick: () -> Unit = {},
    onTogglePlay: () -> Unit,
    onSplit: (() -> Unit)? = null,
    onStepBackward: (() -> Unit)? = null,
    onStepForward: (() -> Unit)? = null,
    onToggleMagneticSnap: () -> Unit = {},
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTransition: (() -> Unit)? = null
) {
    val barBackground = Color(0xFF101014)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(barBackground)
            .testTag("editor_transport_row")
    ) {
        // [POSITION-LOCKED — user approved 2026-11-21]
        // The live timecode chip sits at the transport bar's LEFT edge (it
        // moved here from the info strip below). Do not relocate it.
        if (timecodeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E1F24))
                    .clickable(onClick = onTimecodeClick)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("editor_timecode_chip")
            ) {
                Text(
                    text = timecodeText,
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // [POSITION-LOCKED — user approved 2026-11-21]
        // Center: Play/Pause button sits EXACTLY on the bar's centre line;
        // the scissors/frame-step group is balanced with a mirror spacer so
        // the play button never drifts off centre. Keep the mirror in sync
        // when editing the left group.
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

            // Mirror of the left-side extras (scissors + -1F) so the play
            // button stays exactly on the horizontal centre of the bar.
            if (onSplit != null && onStepBackward != null) {
                Spacer(modifier = Modifier.width(40.dp))
            } else if (onSplit != null) {
                Spacer(modifier = Modifier.width(36.dp))
            } else if (onStepBackward != null) {
                Spacer(modifier = Modifier.width(32.dp))
            }
        }

        // Right: Magnetic Snap [ON], Undo, Redo
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Magnetic Snapping / Track Magnet toggle
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (magneticSnap) Color(0xFF26E5D7).copy(alpha = 0.25f) else Color(0xFF1E1F24))
                    .border(
                        width = 1.2.dp,
                        color = if (magneticSnap) Color(0xFF26E5D7) else Color(0xFF33353E),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                IconButton(
                    onClick = onToggleMagneticSnap,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("editor_magnetic_snap_button")
                ) {
                    CapCutIconMagnetOn(
                        isOn = magneticSnap,
                        tint = if (magneticSnap) Color(0xFF26E5D7) else Color.White,
                        size = 20.dp
                    )
                }
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
    }
}

