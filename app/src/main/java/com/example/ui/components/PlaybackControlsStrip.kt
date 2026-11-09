package com.example.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import com.example.model.MediaClip
import com.example.ui.theme.PencilTheme

/** Playback transport, quick split, magnetic snap, keyframe and zoom controls. */
@Composable
fun PlaybackControlsStrip(
    isPlaying: Boolean,
    zoom: Float,
    magneticSnap: Boolean,
    onTogglePlay: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onQuickSplit: () -> Unit,
    onBeatSync: (() -> Unit)? = null,
    onAddMarker: (() -> Unit)? = null,
    onPreviousMarker: (() -> Unit)? = null,
    onNextMarker: (() -> Unit)? = null,
    markerCount: Int = 0,
    onToggleMultiSelect: (() -> Unit)? = null,
    isMultiSelect: Boolean = false,
    onToggleMagneticSnap: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    selectedClip: MediaClip? = null,
    currentPositionMs: Long = 0L,
    onToggleKeyframe: () -> Unit = {}
) {
    val colors = PencilTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(colors.surface)
            .border(1.2.dp, colors.borderSubtle)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onStepBackward, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Step Back",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated)
                .border(1.8.dp, colors.border, CircleShape)
                .clickable { onTogglePlay() }
                .testTag("editor_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = onStepForward, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Step Forward",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (selectedClip != null) {
            val hasKeyframe = selectedClip.keyframes.any {
                kotlin.math.abs((it.timestampMs - currentPositionMs).toDouble()) < 150.0
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (hasKeyframe) Color(0xFFFF3B30).copy(alpha = 0.2f) else colors.surfaceElevated
                    )
                    .border(
                        1.2.dp,
                        if (hasKeyframe) Color(0xFFFF3B30) else colors.accentCyan,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleKeyframe() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (hasKeyframe) "◆ -" else "◆ +",
                    color = if (hasKeyframe) Color(0xFFFF3B30) else colors.accentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.3.dp, colors.border, RoundedCornerShape(8.dp))
                .clickable { onQuickSplit() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("editor_quick_split")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Quick Split",
                    tint = colors.accentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Split", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (onBeatSync != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .border(1.3.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable { onBeatSync() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Beat Sync",
                        tint = colors.accentPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Beat", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (onAddMarker != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .border(1.3.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable { onAddMarker() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("editor_add_marker")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Marker",
                        tint = colors.accentYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "M:$markerCount",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (onPreviousMarker != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .border(1.3.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable { onPreviousMarker() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("editor_prev_marker")
            ) {
                Text("M⏴", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        if (onNextMarker != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .border(1.3.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable { onNextMarker() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("editor_next_marker")
            ) {
                Text("M⏵", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        if (onToggleMultiSelect != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isMultiSelect) colors.accentCyan.copy(alpha = 0.18f) else colors.surfaceElevated)
                    .border(
                        1.3.dp,
                        if (isMultiSelect) colors.accentCyan else colors.border,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleMultiSelect() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("editor_toggle_multiselect")
            ) {
                Text(
                    text = if (isMultiSelect) "MS ON" else "MS OFF",
                    color = if (isMultiSelect) colors.accentCyan else colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (magneticSnap) colors.surfaceElevated else colors.surface)
                .border(
                    width = 1.3.dp,
                    color = if (magneticSnap) colors.border else colors.borderSubtle,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onToggleMagneticSnap() }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("editor_toggle_snap")
        ) {
            Text(
                text = if (magneticSnap) "🧲 SNAP" else "🧲 OFF",
                color = if (magneticSnap) colors.textPrimary else colors.textTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = { onZoomChanged(zoom - 0.25f) }, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = "${String.format("%.1f", zoom)}x",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = { onZoomChanged(zoom + 0.25f) }, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.ZoomIn,
                    contentDescription = "Zoom In",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
