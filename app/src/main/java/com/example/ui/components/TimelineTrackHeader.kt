package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/** One row of the fixed track header gutter. Height matches the track row. */
data class TimelineHeaderRow(
    val title: String,
    val heightDp: Dp,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val isSolo: Boolean = false,
    /** Buttons the row really supports. Text/effect rows are labels only. */
    val canMute: Boolean = false,
    val canLock: Boolean = false,
    val canSolo: Boolean = false,
    val icon: ImageVector = Icons.Default.VideoLibrary,
    val onMute: (() -> Unit)? = null,
    val onLock: (() -> Unit)? = null,
    val onSolo: (() -> Unit)? = null
)

/**
 * Fixed left-hand track controls.
 *
 * Mute / lock / solo are real: the header calls back into the project, which
 * drives the composition gain and blocks edits on locked tracks. The old shared
 * header only toggled a local icon, so it was a lie.
 */
@Composable
fun TimelineTrackHeaders(rows: List<TimelineHeaderRow>) {
    val colors = PencilTheme.colors
    rows.forEach { row ->
        Box(
            modifier = Modifier
                .height(row.heightDp)
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 3.dp, vertical = 1.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = row.icon,
                        contentDescription = row.title,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = row.title,
                        color = colors.textPrimary,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (row.canMute) {
                        HeaderToggle(
                            selected = row.isMuted,
                            on = Icons.Default.VolumeOff,
                            off = Icons.Default.VolumeUp,
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
    activeColor: androidx.compose.ui.graphics.Color
) {
    val colors = PencilTheme.colors
    Box(
        modifier = Modifier
            .size(17.dp)
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
