package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.engine.SnapCandidate
import com.example.engine.SnapEngine
import com.example.engine.TimelineGeometry
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip

/**
 * Draggable playhead needle matching the reference CapCut screenshot:
 * A clean, solid 2dp white vertical line passing through ruler and tracks.
 */
@Composable
fun TimelinePlayhead(
    positionMs: Long,
    totalMs: Long,
    zoom: Float,
    scrollState: ScrollState,
    fps: Int,
    magneticSnap: Boolean,
    snapCandidates: List<SnapCandidate>,
    snapThresholdFrames: Long,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current
    val safeZoom = zoom.coerceAtLeast(0.01f)

    val scrollPx = scrollState.value
    val xDp = TimelineGeometry.screenXDp(
        positionMs = positionMs,
        zoom = safeZoom,
        scrollPx = scrollPx,
        density = density.density
    )

    val latestPosition by rememberUpdatedState(positionMs)
    val latestZoom by rememberUpdatedState(safeZoom)
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestOnScrubStart by rememberUpdatedState(onScrubStart)
    val latestOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val latestSnap by rememberUpdatedState(magneticSnap)
    val latestCandidates by rememberUpdatedState(snapCandidates)
    val latestThreshold by rememberUpdatedState(snapThresholdFrames)

    var dragging by remember { mutableStateOf(false) }
    var dragAnchorMs by remember { mutableLongStateOf(0L) }
    var dragAccumPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width((TimelineGeometry.PLAYHEAD_HIT_HALF_DP * 2f).dp)
            .fillMaxHeight()
            .offset(x = (xDp - TimelineGeometry.PLAYHEAD_HIT_HALF_DP).dp)
            .graphicsLayer { scaleX = if (dragging) 1.05f else 1f }
            .pointerInput(totalMs, fps) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        dragAnchorMs = latestPosition
                        dragAccumPx = 0f
                        latestOnScrubStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumPx += dragAmount.x
                        val rawMs = TimelineGeometry.positionMsAfterDrag(
                            anchorMs = dragAnchorMs,
                            dragPx = dragAccumPx,
                            density = density.density,
                            zoom = latestZoom
                        ).coerceIn(0L, totalMs)
                        val target = snapTargetMs(
                            rawMs = rawMs,
                            fps = fps,
                            totalMs = totalMs,
                            enabled = latestSnap,
                            candidates = latestCandidates,
                            thresholdFrames = latestThreshold
                        )
                        latestOnSeek(target)
                        TimelineAutoScroll.toReveal(
                            targetMs = target,
                            zoom = latestZoom,
                            density = density.density,
                            scrollState = scrollState
                        )
                    },
                    onDragEnd = {
                        dragging = false
                        dragAccumPx = 0f
                        latestOnScrubEnd()
                    },
                    onDragCancel = {
                        dragging = false
                        dragAccumPx = 0f
                        latestOnScrubEnd()
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        if (!colors.isPencilSketch) {
            // CapCut Solid White Center Needle
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(if (dragging) 2.5.dp else 2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )

            // Small rounded top anchor cap
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 4.dp, height = 5.dp)
                    .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                    .background(Color.White)
            )
        } else {
            // Pencil theme
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 15.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(colors.playhead)
            )
            SharpenedPencilTip(modifier = Modifier.align(Alignment.TopCenter).offset(y = 12.dp))
        }
    }
}

/** Snaps a raw seek position to nearby clip edges, never to itself. */
internal fun snapTargetMs(
    rawMs: Long,
    fps: Int,
    totalMs: Long,
    enabled: Boolean,
    candidates: List<SnapCandidate>,
    thresholdFrames: Long
): Long {
    val safeFps = fps.coerceIn(1, 120)
    val rawFrame = (rawMs.coerceIn(0L, totalMs) * safeFps / 1_000L)
    val snapped = SnapEngine.snap(
        targetFrame = rawFrame,
        candidates = candidates,
        thresholdFrames = thresholdFrames,
        enabled = enabled
    )
    return (snapped.frame * 1_000L / safeFps).coerceIn(0L, totalMs)
}
