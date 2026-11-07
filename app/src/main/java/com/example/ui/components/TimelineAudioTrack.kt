package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.AudioClip
import com.example.ui.theme.PencilTheme
import kotlin.math.roundToLong

/**
 * Music / voice-over track.
 *
 * Each audio clip sits on its own row and can be long-press dragged along the
 * timeline; the drop is snapped and clamped by `EditorViewModel.moveAudioClip`.
 */
@Composable
fun TimelineAudioTrack(
    tracks: List<AudioClip>,
    msToDp: Float,
    onMoveClip: (audioId: String, newStartMs: Long) -> Unit = { _, _ -> },
    onToggleMute: (audioId: String, muted: Boolean) -> Unit = { _, _ -> },
    selectedAudioId: String? = null
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current

    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        tracks.forEach { audio ->
            val isSelected = audio.id == selectedAudioId
            val isDragging = draggedId == audio.id
            val startOffsetDp = (audio.startTimelineMs * msToDp).dp
            val widthDp = (audio.durationMs * msToDp).dp.coerceAtLeast(54.dp)

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
                        colors.accentPurple.copy(
                            alpha = if (isSelected || isDragging) 0.38f else 0.25f
                        )
                    )
                    .border(
                        width = if (isSelected || isDragging) 2.2.dp else 1.2.dp,
                        color = if (isDragging) colors.accentYellow else colors.accentPurple,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .pointerInput(audio.id, msToDp) {
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
                                    onMoveClip(
                                        audio.id,
                                        (audio.startTimelineMs + deltaMs).coerceAtLeast(0L)
                                    )
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
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio",
                        tint = if (audio.isMuted) colors.textTertiary else colors.accentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = audio.title,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Mute state is toggled from the header button, not from the chip itself, so
 * the audio chip keeps the whole surface available for the long-press drag.
 */
