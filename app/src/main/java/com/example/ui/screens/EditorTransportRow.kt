package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PencilTheme

/**
 * CapCut transport row: fullscreen on the left, a centred play/pause bubble,
 * then transition, undo and redo on the right. Extra editing tools (markers,
 * beat sync, multi-select, zoom) live in the bottom "Tools" panel so this row
 * matches the reference layout exactly.
 */
@Composable
internal fun EditorTransportRow(
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onTogglePlay: () -> Unit,
    onFullscreen: () -> Unit,
    onTransition: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(colors.background)
            .testTag("editor_transport_row")
    ) {
        // Left: fullscreen preview.
        IconButton(
            onClick = onFullscreen,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .testTag("editor_fullscreen_button")
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Fullscreen preview",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Centre: play/pause bubble.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .clickable { onTogglePlay() }
                .testTag("editor_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = colors.textPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        // Right: transition + undo + redo.
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTransition,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("editor_transition_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterNone,
                    contentDescription = "Add transition",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
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
                    tint = if (canUndo) colors.textPrimary else colors.textTertiary.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
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
                    tint = if (canRedo) colors.textPrimary else colors.textTertiary.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
