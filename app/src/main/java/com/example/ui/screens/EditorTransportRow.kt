package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import com.example.ui.components.CapCutIconFullscreen
import com.example.ui.components.CapCutIconMagnetOn
import com.example.ui.components.CapCutIconPlayTriangle

/**
 * CapCut transport row:
 * - Left: Fullscreen preview button (corner brackets) + canvas aspect ratio chip
 * - Center: Play/Pause button (clean outline triangle)
 * - Right: Magnetic snap toggle (overlapping frames with "ON"), Undo, Redo
 *
 * The aspect ratio chip sits on this bar (below the preview) instead of being
 * painted over the video frame, so the imported media is never obscured.
 */
@Composable
internal fun EditorTransportRow(
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    magneticSnap: Boolean = true,
    aspectRatioLabel: String = "16:9",
    onAspectRatioClick: (() -> Unit)? = null,
    onTogglePlay: () -> Unit,
    onFullscreen: () -> Unit,
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
        // Left: Fullscreen preview button
        IconButton(
            onClick = onFullscreen,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(46.dp)
                .testTag("editor_fullscreen_button")
        ) {
            CapCutIconFullscreen(
                tint = Color.White,
                size = 20.dp
            )
        }

        // Left (after fullscreen): canvas / imported-media aspect ratio chip.
        // Tapping it opens the ratio picker, just like the top bar format action.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF2A2A31))
                .then(
                    if (onAspectRatioClick != null) {
                        Modifier.clickable(onClick = onAspectRatioClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 7.dp, vertical = 4.dp)
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

        // Center: Clean outline play/pause button (no circle background in CapCut reference)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
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
    }
}
