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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.MediaClip
import com.example.model.TransitionType
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.pencilHatching
import com.example.ui.theme.sketchPencilBorder

/**
 * Main video track. Tapping selects a clip; long-press then drag reorders it.
 *
 * A normal horizontal drag is left untouched for the timeline's scroll view —
 * only a deliberate long press captures the gesture.
 */
@Composable
fun TimelineVideoTrack(
    clips: List<MediaClip>,
    selectedClipId: String?,
    msToDp: Float,
    onSelectClip: (String) -> Unit,
    onTransitionClick: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current

    // Slot starts include the 26.dp transition button between clips so a drop
    // lands on the index the user actually sees.
    val clipWidthDps = clips.map { (it.durationMs * msToDp).coerceAtLeast(48f) }
    val transitionDp = 26f

    fun slotStart(index: Int): Float {
        var acc = 0f
        for (j in 0 until index) acc += clipWidthDps[j] + transitionDp
        return acc
    }

    fun targetIndex(from: Int, dragDp: Float): Int {
        val center = slotStart(from) + dragDp + clipWidthDps[from] / 2f
        var target = 0
        clips.forEachIndexed { i, _ ->
            if (i == from) return@forEachIndexed
            if (slotStart(i) + clipWidthDps[i] / 2f < center) target += 1
        }
        return target.coerceIn(0, clips.lastIndex)
    }

    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        clips.forEachIndexed { index, clip ->
            val fromIndex = index
            val isSelected = clip.id == selectedClipId
            val isDragging = draggedId == clip.id
            val clipWidthDp = clipWidthDps[index].dp

            Box(
                modifier = Modifier
                    .width(clipWidthDp)
                    .fillMaxHeight()
                    .zIndex(if (isDragging) 20f else 0f)
                    .graphicsLayer {
                        translationX = if (isDragging) dragOffsetPx else 0f
                        shadowElevation = if (isDragging) 14f else 0f
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 1.03f else 1f
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.clipSelected else colors.clipBg)
                    .pencilHatching(
                        color = if (isSelected) colors.accentCyan.copy(alpha = 0.18f) else colors.border.copy(alpha = 0.08f),
                        spacing = 7.dp,
                        enabled = colors.isPencilSketch
                    )
                    .sketchPencilBorder(
                        strokeWidth = if (isSelected || isDragging) 2.2.dp else 1.2.dp,
                        color = when {
                            isDragging -> colors.accentYellow
                            isSelected -> colors.accentCyan
                            else -> colors.borderSubtle
                        },
                        cornerRadius = 8.dp,
                        isPencilStyle = colors.isPencilSketch
                    )
                    .clickable { onSelectClip(clip.id) }
                    .pointerInput(clip.id, clips.size, clipWidthDps) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragOffsetPx = 0f
                                draggedId = clip.id
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetPx += dragAmount.x
                            },
                            onDragEnd = {
                                val dragDp = dragOffsetPx / density.density
                                val to = targetIndex(fromIndex, dragDp)
                                if (draggedId == clip.id && to != fromIndex) {
                                    onMoveClip(fromIndex, to)
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
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 2.5.dp.toPx()
                    val spacing = 2.5.dp.toPx()
                    var currX = 0f
                    var i = 0
                    val waveform = clip.waveform
                    val waveformSize = waveform.size
                    while (currX < size.width) {
                        val factor = if (waveformSize > 0) waveform[i % waveformSize] else 0.5f
                        val barHeight = size.height * 0.42f * factor
                        drawLine(
                            color = if (isSelected) {
                                colors.border.copy(alpha = 0.55f)
                            } else {
                                colors.textTertiary.copy(alpha = 0.4f)
                            },
                            start = Offset(currX, size.height - barHeight),
                            end = Offset(currX, size.height),
                            strokeWidth = barWidth
                        )
                        currX += barWidth + spacing
                        i++
                    }
                }

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
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (clip.isReversed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFF2D55))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "◀ REV",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    if (clip.chromaKey.isEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF34C759))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "🟢 KEY",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = clip.title,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%.1fs", clip.durationMs / 1000f),
                            color = colors.accentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (clip.speed != 1.0f) {
                            Text(
                                text = "${clip.speed}x",
                                color = colors.accentYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (clip.keyframes.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        clip.keyframes.forEach { kf ->
                            val kfRatio = if (clip.durationMs > 0) {
                                (kf.timestampMs.toFloat() / clip.durationMs).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            val kfOffsetDp = (clipWidthDp.value * kfRatio).dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .offset(x = kfOffsetDp - 4.dp, y = (-2).dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentPink)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(
                                colors.handle,
                                RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(
                                colors.handle,
                                RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            )
                    )
                }
            }

            if (index < clips.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceElevated)
                        .border(1.2.dp, colors.border, RoundedCornerShape(6.dp))
                        .clickable { onTransitionClick(clip.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Transition",
                        tint = if (clip.transition != TransitionType.NONE) {
                            colors.accentCyan
                        } else {
                            colors.textTertiary
                        },
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.2.dp, colors.border, RoundedCornerShape(8.dp))
                .clickable { onAddMediaClick() }
                .testTag("add_clip_to_timeline_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Clip",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
