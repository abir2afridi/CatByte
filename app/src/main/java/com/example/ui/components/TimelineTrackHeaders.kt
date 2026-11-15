package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.EditorTimecodeText
import com.example.ui.theme.PencilTheme

/** Legacy support for unit tests and multi-track toggle bars */
@Composable
fun TimelineTrackHeaders(rows: List<TimelineHeaderRow>) {
    val colors = PencilTheme.colors
    rows.forEach { row ->
        Box(
            modifier = Modifier
                .height(row.heightDp)
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = row.icon,
                        contentDescription = row.title,
                        tint = colors.accentCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = row.title,
                        color = colors.textPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (row.canMute) {
                        HeaderToggle(
                            selected = row.isMuted,
                            on = Icons.AutoMirrored.Filled.VolumeOff,
                            off = Icons.AutoMirrored.Filled.VolumeUp,
                            description = "Mute track",
                            onClick = { row.onMute?.invoke() },
                            activeColor = colors.accentYellow
                        )
                    }
                    if (row.canLock) {
                        HeaderToggle(
                            selected = row.isLocked,
                            on = Icons.Default.Lock,
                            off = Icons.Default.LockOpen,
                            description = "Lock track",
                            onClick = { row.onLock?.invoke() },
                            activeColor = colors.accentPink
                        )
                    }
                    if (row.canSolo) {
                        HeaderToggle(
                            selected = row.isSolo,
                            on = Icons.Default.Headset,
                            off = Icons.Default.Headset,
                            description = "Solo track",
                            onClick = { row.onSolo?.invoke() },
                            activeColor = colors.accentCyan
                        )
                    }
                    if (row.canReorder) {
                        HeaderToggle(
                            selected = true,
                            on = Icons.Default.KeyboardArrowUp,
                            off = Icons.Default.KeyboardArrowUp,
                            description = "Move track up",
                            onClick = { row.onMoveUp?.invoke() },
                            activeColor = colors.accentCyan
                        )
                        HeaderToggle(
                            selected = true,
                            on = Icons.Default.KeyboardArrowDown,
                            off = Icons.Default.KeyboardArrowDown,
                            description = "Move track down",
                            onClick = { row.onMoveDown?.invoke() },
                            activeColor = colors.accentCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderToggle(
    selected: Boolean,
    on: ImageVector,
    off: ImageVector,
    description: String,
    onClick: () -> Unit,
    activeColor: Color
) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) colors.surfaceElevated else colors.surfaceVariant)
            .clickable { onClick() }
            .testTag("track_header_$description"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) on else off,
            contentDescription = description,
            tint = if (selected) activeColor else colors.textTertiary,
            modifier = Modifier.size(11.dp)
        )
    }
}
