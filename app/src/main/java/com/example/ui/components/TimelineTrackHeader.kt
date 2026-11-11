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

/** One row of the fixed track header gutter. Height matches the track row. */
data class TimelineHeaderRow(
    val title: String,
    val heightDp: Dp,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val isSolo: Boolean = false,
    val canMute: Boolean = false,
    val canLock: Boolean = false,
    val canSolo: Boolean = false,
    val icon: ImageVector = Icons.Default.VideoLibrary,
    val onMute: (() -> Unit)? = null,
    val onLock: (() -> Unit)? = null,
    val onSolo: (() -> Unit)? = null,
    val canReorder: Boolean = false,
    val onMoveUp: (() -> Unit)? = null,
    val onMoveDown: (() -> Unit)? = null
)

/**
 * CapCut-exact left gutter for timeline:
 * - Top level (ruler height): Timecode display (00:01.8 / 00:16.0)
 * - Main Track: "Mute clip" speaker + "Cover" thumbnail
 * - Audio Track: Square with Music note ♪
 * - Text Track: Square with T
 * - Overlay Track: Square with Video icon
 *
 * Width is 108.dp so the sub-second timecode fits on one line; the timeline
 * viewport measures this gutter at runtime, so no other offsets are hard-coded.
 */
@Composable
fun CapCutTimelineLeftGutter(
    currentPositionMs: State<Long>,
    totalDurationMs: Long,
    isMainMuted: Boolean,
    onToggleMainMute: () -> Unit,
    onOpenCover: () -> Unit,
    overlayTrackCount: Int = 0,
    hasAudioTrack: Boolean = true,
    hasTextTrack: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(108.dp)
            .fillMaxHeight()
            .background(Color(0xFF101014))
            .padding(horizontal = 4.dp)
    ) {
        // 1. Ruler level (28.dp): Timecode display (00:01.8 / 00:16.0)
        Box(
            modifier = Modifier
                .height(28.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            GutterTimecode(
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalDurationMs
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 2. Main Track level (height ~62.dp): "Mute clip" and "Cover"
        Row(
            modifier = Modifier
                .height(62.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute clip button (Speaker icon + "Mute\nclip" stacked text)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onToggleMainMute() }
                    .padding(vertical = 2.dp)
                    .testTag("timeline_mute_clip_button")
            ) {
                Icon(
                    imageVector = if (isMainMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute clip",
                    tint = if (isMainMuted) Color(0xFFFF4D4D) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isMainMuted) "Muted" else "Mute\nclip",
                    color = Color(0xFFC5C5C7),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Cover thumbnail card with meadow preview frame and pencil icon
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenCover() }
                    .testTag("timeline_cover_button"),
                contentAlignment = Alignment.Center
            ) {
                // Background video frame (meadow & sky scene)
                CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())

                // Dark subtle overlay gradient for contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )

                // Pencil icon and "Cover" text centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Cover",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cover",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 3. Overlay tracks if any
        repeat(overlayTrackCount) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2026)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Overlay",
                        tint = Color(0xFF00F2FE),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
        }

        // 4. Audio track icon ♪ (aligned under Cover card)
        if (hasAudioTrack) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2026)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio track",
                        tint = Color(0xFFA5A8B2),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
        }

        // 5. Text track icon T (aligned under Cover card)
        if (hasTextTrack) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2026)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T",
                        color = Color(0xFFA5A8B2),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Reads the playhead State inside its own scope, so a scrub tick recomposes
 * only the timecode text instead of the whole gutter (buttons and cover card
 * included). During a drag this runs dozens of times per second.
 */
@Composable
private fun GutterTimecode(
    currentPositionMs: State<Long>,
    totalDurationMs: Long
) {
    EditorTimecodeText(
        currentPositionMs = currentPositionMs.value,
        totalDurationMs = totalDurationMs
    )
}

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
