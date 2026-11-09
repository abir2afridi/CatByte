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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMicros
import com.example.engine.ClipFrames
import com.example.engine.ClipTrimHandle
import com.example.engine.TimelineGeometry
import com.example.model.MediaClip
import kotlin.math.abs

/**
 * Tiled filmstrip frames for one clip.
 *
 * Own composable on purpose: a trim drag changes only the clip's width, and
 * with strong skipping this whole row of Coil images is skipped unless the
 * tile count actually changes — one of the reasons the trim handle stays
 * smooth instead of stuttering.
 */
@Composable
private fun ClipFilmstrip(
    clip: MediaClip,
    frameCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(modifier = modifier) {
        val frameWidthDp = 50.dp
        // Every tile shows a DIFFERENT frame, sampled evenly across the clip's
        // selected source window (in-point .. out-point). The sampling rule
        // (centre of each tile's span, snapped to a 500ms cache grid) lives in
        // engine/ClipFrames.kt so zooming reuses cached frames.
        for (f in 0 until frameCount) {
            if (clip.uriString != null) {
                val model = ImageRequest.Builder(context)
                    .data(clip.uriString)
                    .videoFrameMicros(ClipFrames.sampleSourceMs(clip, f, frameCount) * 1000L)
                    .size(160)
                    .build()
                AsyncImage(
                    model = model,
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
}

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
    onTrimStart: (clipId: String, edge: ClipTrimHandle.Edge) -> Unit = { _, _ -> },
    onTrimPreview: (clipId: String, edge: ClipTrimHandle.Edge, deltaMs: Long) -> Unit = { _, _, _ -> },
    onTrimEnd: (clipId: String, edge: ClipTrimHandle.Edge) -> Unit = { _, _ -> },
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
                        .zIndex(
                            when {
                                isDragging -> 30f
                                // Above the split markers (15f) so the bold trim
                                // frame and handles are never covered.
                                isSelected -> 25f
                                else -> 0f
                            }
                        )
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffsetPx else 0f
                            shadowElevation = if (isDragging) 14f else 0f
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF181A20))
                        .border(
                            width = if (isSelected) 3.dp else 0.8.dp,
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
                    // Tiled filmstrip frames. Extracted so a trim drag — which
                    // only changes the clip's width — does not recompose the
                    // heavy AsyncImage row unless the tile count changes.
                    ClipFilmstrip(
                        clip = clip,
                        frameCount = ((clipWidthDp / 50.dp).toInt()).coerceAtLeast(1),
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                    )

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
                            onDragStart = {
                                onTrimStart(clip.id, ClipTrimHandle.Edge.START)
                            },
                            onDragDelta = { deltaPx ->
                                val deltaMs = TimelineGeometry.dragPxToMs(
                                    deltaPx, density.density, msToDp
                                )
                                onTrimPreview(clip.id, ClipTrimHandle.Edge.START, deltaMs)
                            },
                            onCommit = {
                                onTrimEnd(clip.id, ClipTrimHandle.Edge.START)
                            }
                        )
                        TimelineTrimHandle(
                            edge = ClipTrimHandle.Edge.END,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onDragStart = {
                                onTrimStart(clip.id, ClipTrimHandle.Edge.END)
                            },
                            onDragDelta = { deltaPx ->
                                val deltaMs = TimelineGeometry.dragPxToMs(
                                    deltaPx, density.density, msToDp
                                )
                                onTrimPreview(clip.id, ClipTrimHandle.Edge.END, deltaMs)
                            },
                            onCommit = {
                                onTrimEnd(clip.id, ClipTrimHandle.Edge.END)
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

                // Split marker at the boundary between adjacent clips (zero gap).
                // The hide-when-selected rule lives in SplitBoundaryMarker.
                val nextClip = clips.firstOrNull { abs(it.startTimelineMs - (clip.startTimelineMs + clip.durationMs)) < 50L }
                if (nextClip != null) {
                    val boundaryX = (clip.startTimelineMs + clip.durationMs) * msToDp
                    SplitBoundaryMarker(
                        leftClipId = clip.id,
                        rightClipId = nextClip.id,
                        selectedClipId = selectedClipId,
                        selectedClipIds = selectedClipIds,
                        modifier = Modifier
                            .offset(x = (boundaryX - 11f).dp)
                            .align(Alignment.CenterStart),
                        onClick = { onTransitionClick(clip.id) }
                    )
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
