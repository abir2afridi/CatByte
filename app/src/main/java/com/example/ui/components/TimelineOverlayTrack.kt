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
import androidx.compose.material.icons.filled.Videocam
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
import com.example.model.MediaClip
import com.example.ui.theme.PencilTheme
import kotlin.math.roundToLong

/**
 * Picture-in-picture / overlay track. Clips are positioned by timeline start.
 *
 * Long-press then drag moves a clip along the overlay track; the drop is
 * snapped and clamped by `EditorViewModel.moveOverlayClip`. A normal horizontal
 * drag is left for the timeline pan, exactly like the main track.
 */
@Composable
fun TimelineOverlayTrack(
    clips: List<MediaClip>,
    selectedClipId: String?,
    msToDp: Float,
    onSelectClip: (String) -> Unit,
    onMoveClip: (clipId: String, newStartMs: Long) -> Unit = { _, _ -> }
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current

    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId
            val isDragging = draggedId == clip.id
            val startOffsetDp = (clip.startTimelineMs * msToDp).dp
            val widthDp = (clip.durationMs * msToDp).dp.coerceAtLeast(48.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .zIndex(if (isDragging) 20f else 0f)
                    .graphicsLayer {
                        translationX = if (isDragging) dragOffsetPx else 0f
                        shadowElevation = if (isDragging) 12f else 0f
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentCyan.copy(alpha = if (isSelected) 0.42f else 0.22f))
                    .border(
                        width = if (isSelected || isDragging) 2.2.dp else 1.2.dp,
                        color = if (isDragging) colors.accentYellow else colors.accentCyan,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .pointerInput(clip.id, msToDp) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedId = clip.id
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
                                if (draggedId == clip.id && deltaMs != 0L) {
                                    onMoveClip(
                                        clip.id,
                                        (clip.startTimelineMs + deltaMs).coerceAtLeast(0L)
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
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (clip.keyframes.isNotEmpty()) {
                    clip.keyframes.forEach { kf ->
                        val kfOffsetMs = kf.timestampMs.coerceIn(0L, clip.durationMs)
                        val kfXDp = (kfOffsetMs * msToDp).dp
                        Box(
                            modifier = Modifier
                                .offset(x = (kfXDp - 4.dp).coerceAtLeast(0.dp), y = 4.dp)
                                .size(8.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(Color(0xFFFF3B30), RoundedCornerShape(1.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(1.dp))
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Overlay clip",
                        tint = colors.accentCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = clip.title,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
