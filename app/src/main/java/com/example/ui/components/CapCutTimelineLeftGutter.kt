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
import com.example.model.AudioClip
import com.example.ui.screens.EditorTimecodeText
import com.example.ui.theme.PencilTheme

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
    audioTracks: List<AudioClip> = emptyList(),
    onToggleAudioMute: (String, Boolean) -> Unit = { _, _ -> },
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

        // 4. One audio row per independent clip. The rows deliberately have
        // no gap between them; the single spacer below matches the timeline.
        val audioRows = if (audioTracks.isEmpty()) listOf(null) else audioTracks
        audioRows.forEach { audio ->
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
                        .background(Color(0xFF1E2026))
                        .then(
                            if (audio != null) {
                                Modifier.clickable {
                                    onToggleAudioMute(audio.id, !audio.isMuted)
                                }
                            } else {
                                Modifier
                            }
                        )
                        .testTag(
                            audio?.let { "timeline_audio_header_${it.id}" }
                                ?: "timeline_audio_header_empty"
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            audio == null -> Icons.Default.MusicNote
                            audio.isMuted -> Icons.AutoMirrored.Filled.VolumeOff
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (audio == null) {
                            "Audio track"
                        } else if (audio.isMuted) {
                            "Unmute ${audio.title}"
                        } else {
                            "Mute ${audio.title}"
                        },
                        tint = if (audio?.isMuted == true) Color(0xFFFF6B6B) else Color(0xFFA5A8B2),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))

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
