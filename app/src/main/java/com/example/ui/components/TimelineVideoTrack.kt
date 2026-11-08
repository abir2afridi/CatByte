package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Main video track matching CapCut:
 * Clips are positioned precisely by timeline start (startTimelineMs) with zero gap between split clips,
 * inline split transition buttons floating at boundaries, and the [+] add media button at the end.
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
    val density = LocalDensity.current

    val totalDurationMs = clips.fold(0L) { acc, c -> maxOf(acc, c.startTimelineMs + c.durationMs) }
    val trackWidthDp = (totalDurationMs * msToDp).coerceAtLeast(300f).dp

    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(Color(0xFF101014))
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(trackWidthDp)
                .fillMaxHeight()
        ) {
            clips.forEachIndexed { index, clip ->
                val isSelected = clip.id == selectedClipId || clip.id in selectedClipIds
                val isDragging = draggedId == clip.id
                val clipWidthDp = (clip.durationMs * msToDp).coerceAtLeast(48f).dp
                val startOffsetDp = (clip.startTimelineMs * msToDp).dp

                // Clip Box positioned at exact startTimelineMs (zero gap when split)
                Box(
                    modifier = Modifier
                        .offset(x = startOffsetDp)
                        .width(clipWidthDp)
                        .fillMaxHeight()
                        .zIndex(if (isDragging) 20f else 0f)
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffsetPx else 0f
                            shadowElevation = if (isDragging) 14f else 0f
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

                    // Trim Handles & Duration Badge
                    if (isSelected && !isLocked) {
                        TimelineTrimHandle(
                            edge = ClipTrimHandle.Edge.START,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterStart),
                            onCommit = { deltaPx ->
                                val deltaDp = deltaPx / density.density
                                val deltaMs = (deltaDp / msToDp).roundToLong()
                                if (deltaMs != 0L) onTrimClip(clip.id, ClipTrimHandle.Edge.START, deltaMs)
                            }
                        )
                        TimelineTrimHandle(
                            edge = ClipTrimHandle.Edge.END,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onCommit = { deltaPx ->
                                val deltaDp = deltaPx / density.density
                                val deltaMs = (deltaDp / msToDp).roundToLong()
                                if (deltaMs != 0L) onTrimClip(clip.id, ClipTrimHandle.Edge.END, deltaMs)
                            }
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-22).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val secs = clip.durationMs / 1000f
                            Text(
                                text = String.format(java.util.Locale.US, "%.1fs", secs),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Split transition button at the boundary between adjacent clips (zero gap)
                val nextClip = clips.firstOrNull { abs(it.startTimelineMs - (clip.startTimelineMs + clip.durationMs)) < 50L }
                if (nextClip != null) {
                    val boundaryX = (clip.startTimelineMs + clip.durationMs) * msToDp
                    val transitionOffsetDp = (boundaryX - 11f).dp
                    Box(
                        modifier = Modifier
                            .offset(x = transitionOffsetDp)
                            .size(width = 22.dp, height = 30.dp)
                            .align(Alignment.CenterStart)
                            .zIndex(15f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .border(0.5.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
                            .clickable { onTransitionClick(clip.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .background(Color.Black)
                        )
                    }
                }
            }

            // Add media button at the end of the timeline
            val addBtnOffsetDivider = clips.fold(0L) { acc, c -> maxOf(acc, c.startTimelineMs + c.durationMs) }
            val addBtnOffsetDp = (addBtnOffsetDivider * msToDp + 8f).dp
            Box(
                modifier = Modifier
                    .offset(x = addBtnOffsetDp)
                    .size(width = 44.dp, height = 52.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .clickable { onAddMediaClick() }
                    .testTag("add_clip_to_timeline_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Clip",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
