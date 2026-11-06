package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SnapCandidate
import com.example.engine.SnapEngine
import com.example.engine.TimelineGeometry
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip

/**
 * Draggable playhead needle.
 *
 * It is rendered *outside* the horizontal scroll container and positioned from
 * `scrollPx`, so dragging it never fights the timeline pan. Drag deltas are
 * accumulated from a fixed anchor instead of reading the needle's own moving
 * position — the old code added the moving position to the touch offset, which
 * made the pencil jump and refuse to follow the finger.
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

    // Reading scrollState here (not in the coordinator) keeps scroll-driven
    // recomposition scoped to the needle instead of the whole timeline.
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
            .graphicsLayer { scaleX = if (dragging) 1.08f else 1f }
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
        // Timestamp badge
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-19).dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.surfaceElevated)
                .border(
                    0.8.dp,
                    if (!colors.isPencilSketch) colors.accentCyan else colors.border,
                    RoundedCornerShape(3.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = String.format("%.1fs", positionMs / 1000f),
                color = if (!colors.isPencilSketch) colors.accentCyan else colors.textPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Needle / pencil body
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 10.dp)
                .width(if (dragging) 3.dp else 2.dp)
                .fillMaxHeight()
                .background(colors.playhead)
        )

        if (!colors.isPencilSketch) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 10.dp, height = 14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.playhead)
                    .border(1.dp, colors.accentCyan, RoundedCornerShape(3.dp))
            )
        } else {
            SharpenedPencilTip(modifier = Modifier.align(Alignment.TopCenter))
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
