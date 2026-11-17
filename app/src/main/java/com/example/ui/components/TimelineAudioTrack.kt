package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.AudioClip
import com.example.viewmodel.features.TrackControlFeature
import kotlin.math.roundToLong

/**
 * Independent music / voice-over tracks.
 *
 * Every clip owns one timeline row, which makes overlapping tracks selectable
 * and keeps the per-track mute button aligned with its clip.
 */
@Composable
fun TimelineAudioTrack(
    tracks: List<AudioClip>,
    msToDp: Float,
    timelineDurationMs: Long? = null,
    onMoveClip: (audioId: String, newStartMs: Long) -> Unit = { _, _ -> },
    onToggleMute: (audioId: String, muted: Boolean) -> Unit = { _, _ -> },
    selectedAudioId: String? = null,
    onSelectAudio: (String) -> Unit = {},
    onAddAudioClick: () -> Unit = {}
) {
    val density = LocalDensity.current
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (tracks.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF101014))
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1D1E22))
                        .clickable { onAddAudioClick() }
                        .padding(horizontal = 12.dp)
                        .testTag("timeline_add_audio"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add audio",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Add audio",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            return@Column
        }

        tracks.forEach { audio ->
            val isSelected = audio.id == selectedAudioId
            val isDragging = draggedId == audio.id
            val startOffsetDp = (audio.startTimelineMs * msToDp).dp
            // Keep enough room for the icon, title and the 28dp mute button.
            // A short clip must never push its own primary control off-screen.
            val widthDp = (audio.durationMs * msToDp).dp.coerceAtLeast(72.dp)

            // Dynamic color theme based on audio type:
            val lowerTitle = audio.title.lowercase()
            val themeColor = when {
                lowerTitle.contains("voice") || lowerTitle.contains("rec") || lowerTitle.contains("mic") -> Color(0xFF7C4DFF) // Purple
                lowerTitle.contains("sfx") || lowerTitle.contains("foley") || lowerTitle.contains("effect") -> Color(0xFFFF6D00) // Amber / Coral
                else -> Color(0xFF00B0FF) // Cyan / Blue for Music
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF101014))
                    .padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = startOffsetDp)
                        .width(widthDp)
                        .fillMaxHeight()
                        .zIndex(if (isDragging) 20f else 0f)
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffsetPx else 0f
                            shadowElevation = if (isDragging) 10f else 0f
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            themeColor.copy(
                                alpha = if (isSelected || isDragging) 0.55f else 0.30f
                            )
                        )
                        .border(
                            width = if (isSelected || isDragging) 1.8.dp else 0.8.dp,
                            color = if (isDragging) Color(0xFFFFD600) else themeColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectAudio(audio.id) }
                        .pointerInput(
                            audio.id,
                            audio.startTimelineMs,
                            audio.durationMs,
                            timelineDurationMs,
                            msToDp
                        ) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = audio.id
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetPx += dragAmount.x
                                },
                                onDragEnd = {
                                    val deltaDp = dragOffsetPx / density.density
                                    val deltaMs = if (msToDp > 0f) {
                                        (deltaDp / msToDp).roundToLong()
                                    } else {
                                        0L
                                    }
                                    if (draggedId == audio.id && deltaMs != 0L) {
                                        val desiredStart = audio.startTimelineMs + deltaMs
                                        val start = timelineDurationMs?.let { totalMs ->
                                            val maxStart = TrackControlFeature.maxAudioStartMs(
                                                timelineDurationMs = totalMs,
                                                clipDurationMs = audio.durationMs
                                            )
                                            desiredStart.coerceIn(0L, maxStart)
                                        } ?: desiredStart.coerceAtLeast(0L)
                                        if (start != audio.startTimelineMs) {
                                            onMoveClip(audio.id, start)
                                        }
                                    }
                                    draggedId = null
                                    dragOffsetPx = 0f
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffsetPx = 0f
                                }
                            )
                        }
                        .padding(start = 8.dp, end = 2.dp)
                        .testTag("timeline_audio_clip_${audio.id}"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Real decoded envelope of the source file, drawn
                    // symmetrically like a studio mixer strip. Clips whose
                    // waveform has not been decoded yet stay flat instead of
                    // showing a decorative fake pattern.
                    if (audio.waveform.isNotEmpty()) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("timeline_audio_waveform_${audio.id}")
                        ) {
                            val barWidth = 2.dp.toPx()
                            val gap = 2.dp.toPx()
                            val centerY = size.height / 2f
                            var x = 0f
                            var i = 0
                            val waveform = audio.waveform
                            val count = waveform.size
                            while (x < size.width) {
                                val factor = waveform[i % count].coerceIn(0f, 1f)
                                val half = size.height * 0.42f * factor
                                drawLine(
                                    color = Color.White.copy(alpha = 0.45f),
                                    start = Offset(x, centerY - half),
                                    end = Offset(x, centerY + half),
                                    strokeWidth = barWidth
                                )
                                x += barWidth + gap
                                i++
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio",
                            tint = if (audio.isMuted) Color.Gray else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = audio.title,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (audio.isMuted) "🔇 Muted" else "🔊 ${(audio.volume * 100).toInt()}%",
                            color = if (audio.isMuted) Color.LightGray else Color(0xFF26E5D7),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onToggleMute(audio.id, !audio.isMuted) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("timeline_audio_mute_${audio.id}")
                        ) {
                            Icon(
                                imageVector = if (audio.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (audio.isMuted) "Unmute audio" else "Mute audio",
                                tint = if (audio.isMuted) Color(0xFFFF6B6B) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
