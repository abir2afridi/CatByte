package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
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
import com.example.ui.theme.PencilTheme

/**
 * Extra timeline utilities that do not exist in the reference top/transport
 * rows: beat sync, markers, multi-select, magnetic snap, zoom and keyframes.
 * Keeping them in one panel keeps the CapCut layout intact.
 */
@Composable
fun TimelineToolsSheet(
    isMultiSelect: Boolean,
    magneticSnap: Boolean,
    zoom: Float,
    markerCount: Int,
    hasSelectedClip: Boolean,
    onBeatSync: () -> Unit,
    onAddMarker: () -> Unit,
    onPreviousMarker: () -> Unit,
    onNextMarker: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onToggleMagneticSnap: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    onToggleKeyframe: () -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("timeline_tools_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textTertiary.copy(alpha = 0.4f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Timeline tools",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolChip("Beat sync", false) { onBeatSync() }
            ToolChip("+ Marker", false, "marker_add") { onAddMarker() }
            ToolChip("◀ Marker", false, "marker_prev") { onPreviousMarker() }
            ToolChip("Marker ▶", false, "marker_next") { onNextMarker() }
            ToolChip("Markers: $markerCount", markerCount > 0) { }
            ToolChip("Multi-select", isMultiSelect, "multiselect") { onToggleMultiSelect() }
            ToolChip("Snap", magneticSnap, "snap") { onToggleMagneticSnap() }
            ToolChip("Keyframe", false, "keyframe") {
                if (hasSelectedClip) onToggleKeyframe()
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Zoom", color = colors.textSecondary, fontSize = 11.sp)
            Slider(
                value = zoom,
                onValueChange = onZoomChanged,
                valueRange = 0.5f..3f,
                modifier = Modifier.weight(1f)
            )
            Text(String.format("%.1fx", zoom), color = colors.accentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    active: Boolean,
    tag: String = label.lowercase().replace(" ", "_").replace(":", ""),
    onClick: () -> Unit
) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) colors.accentCyan.copy(alpha = 0.2f) else colors.surfaceElevated)
            .border(
                1.dp,
                if (active) colors.accentCyan else colors.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 7.dp)
            .testTag("tools_$tag")
    ) {
        Text(
            label,
            color = if (active) colors.accentCyan else colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
