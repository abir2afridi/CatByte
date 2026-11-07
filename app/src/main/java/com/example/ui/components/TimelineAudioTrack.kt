package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Color
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
 * Music / voice-over track matching CapCut:
 * Renders audio clips or the "+ Add audio" strip button.
 */
@Composable
fun TimelineAudioTrack(
    tracks: List<AudioClip>,
    msToDp: Float,
    onMoveClip: (audioId: String, newStartMs: Long) -> Unit = { _, _ -> },
    onToggleMute: (audioId: String, muted: Boolean) -> Unit = { _, _ -> },
    selectedAudioId: String? = null,
    onAddAudioClick: () -> Unit = {}
) {
    val density = LocalDensity.current
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color(0xFF101014))
            .padding(vertical = 2.dp)
    ) {
        if (tracks.isEmpty()) {
            Row(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1D1E22))
                    .clickable { onAddAudioClick() }
                    .padding(horizontal = 12.dp),
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
                        Color(0xFF7C4DFF).copy(
                            alpha = if (isSelected || isDragging) 0.55f else 0.35f
                        )
                    )
                    .border(
                        width = if (isSelected || isDragging) 1.8.dp else 0.8.dp,
                        color = if (isDragging) Color(0xFFFFD600) else Color(0xFF7C4DFF),
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
                        tint = if (audio.isMuted) Color.Gray else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = audio.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
