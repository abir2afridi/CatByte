package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CompareArrows
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.engine.ClipTrimHandle
import com.example.model.MediaClip
import com.example.model.TransitionType
import com.example.ui.theme.PencilTheme
import kotlin.math.roundToLong

/**
 * Main video track matching the CapCut screenshot:
 * Tiled thumbnail filmstrip, selected clip borders, transition buttons, and
 * the clean white [+] add media button at the end.
 */
@Composable
fun TimelineVideoTrack(
    clips: List<MediaClip>,
    selectedClipId: String?,
    msToDp: Float,
    onSelectClip: (String) -> Unit,
    onTransitionClick: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onMoveClip: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onTrimClip: (clipId: String, edge: ClipTrimHandle.Edge, deltaMs: Long) -> Unit = { _, _, _ -> },
    fps: Int = 30,
    isLocked: Boolean = false,
    isMultiSelect: Boolean = false,
    selectedClipIds: Set<String> = emptySet(),
    onToggleClipSelection: (String) -> Unit = {}
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current

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
            .background(Color(0xFF101014))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        clips.forEachIndexed { index, clip ->
            val fromIndex = index
            val isSelected = clip.id == selectedClipId || clip.id in selectedClipIds
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
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF181A20))
                    .border(
                        width = if (isSelected) 2.dp else 0.8.dp,
                        color = when {
                            isDragging -> Color(0xFFFFD600)
                            isSelected -> Color.White
                            else -> Color(0xFF282B34)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        if (isMultiSelect) {
                            onToggleClipSelection(clip.id)
                        } else {
                            onSelectClip(clip.id)
                        }
                    }
                    .pointerInput(clip.id, clips.size, clipWidthDps, isLocked) {
                        if (isLocked) return@pointerInput
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
            ) {
                // Tiled filmstrip frames
                Row(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                ) {
                    val frameWidthDp = 50.dp
                    val frameCount = ((clipWidthDp / frameWidthDp).toInt()).coerceAtLeast(1)
                    for (f in 0 until frameCount) {
                        if (clip.uriString != null) {
                            AsyncImage(
                                model = clip.uriString,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(frameWidthDp)
                                    .fillMaxHeight()
                                    .border(0.4.dp, Color.Black.copy(alpha = 0.25f))
                            )
                        } else if (clip.drawableResId != null) {
                            Image(
                                painter = painterResource(clip.drawableResId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(frameWidthDp)
                                    .fillMaxHeight()
                                    .border(0.4.dp, Color.Black.copy(alpha = 0.25f))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(frameWidthDp)
                                    .fillMaxHeight()
                                    .border(0.4.dp, Color.Black.copy(alpha = 0.25f))
                            ) {
                                CapCutMeadowCanvas(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }

                // Waveform overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 2.dp.toPx()
                    val spacing = 2.dp.toPx()
                    var currX = 0f
                    var i = 0
                    val waveform = clip.waveform
                    val waveformSize = waveform.size
                    while (currX < size.width) {
                        val factor = if (waveformSize > 0) waveform[i % waveformSize] else 0.5f
                        val barHeight = size.height * 0.35f * factor
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(currX, size.height - barHeight),
                            end = Offset(currX, size.height),
                            strokeWidth = barWidth
                        )
                        currX += barWidth + spacing
                        i++
                    }
                }

                // Trim Handles
                if (isSelected && !isLocked) {
                    TimelineTrimHandle(
                        edge = ClipTrimHandle.Edge.START,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onCommit = { deltaPx ->
                            val deltaDp = deltaPx / density.density
                            val deltaMs = (deltaDp / msToDp).roundToLong()
                            if (deltaMs != 0L) onTrimClip(
                                clip.id,
                                ClipTrimHandle.Edge.START,
                                deltaMs
                            )
                        }
                    )
                    TimelineTrimHandle(
                        edge = ClipTrimHandle.Edge.END,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onCommit = { deltaPx ->
                            val deltaDp = deltaPx / density.density
                            val deltaMs = (deltaDp / msToDp).roundToLong()
                            if (deltaMs != 0L) onTrimClip(
                                clip.id,
                                ClipTrimHandle.Edge.END,
                                deltaMs
                            )
                        }
                    )
                }
            }

            if (index < clips.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF22242C))
                        .border(1.dp, Color(0xFF323640), RoundedCornerShape(6.dp))
                        .clickable { onTransitionClick(clip.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "Transition",
                        tint = if (clip.transition != TransitionType.NONE) Color(0xFF00F2FE) else Color(0xFFA0A0A8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // White square button with black [+] at the end of clips (CapCut style)
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .clickable { onAddMediaClick() }
                .testTag("add_clip_to_timeline_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Clip",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
