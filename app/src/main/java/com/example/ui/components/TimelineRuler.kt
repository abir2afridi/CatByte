package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SnapCandidate
import com.example.engine.TimelineGeometry
import com.example.ui.theme.PencilTheme

/**
 * Time ruler with tap-and-drag scrubbing.
 *
 * Dragging the ruler is a deliberate seek gesture, so it consumes the pointer
 * and the surrounding scroll view is free to pan everywhere else. Tick x
 * positions are converted from dp to pixels before drawing (the old code drew
 * dp values directly into a pixel canvas, which compressed the ruler on every
 * high-density phone).
 */
@Composable
fun TimelineRuler(
    totalMs: Long,
    zoom: Float,
    fps: Int,
    magneticSnap: Boolean,
    snapCandidates: List<SnapCandidate>,
    snapThresholdFrames: Long,
    scrollState: ScrollState,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current
    val safeZoom = zoom.coerceAtLeast(0.01f)

    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestOnScrubStart by rememberUpdatedState(onScrubStart)
    val latestOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val latestSnap by rememberUpdatedState(magneticSnap)
    val latestCandidates by rememberUpdatedState(snapCandidates)
    val latestThreshold by rememberUpdatedState(snapThresholdFrames)

    val labelStepMs = when {
        TimelineGeometry.dpPerMs(safeZoom) * 1_000f >= 48f -> 1_000L
        TimelineGeometry.dpPerMs(safeZoom) * 1_000f >= 24f -> 2_000L
        else -> 5_000L
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(colors.surfaceElevated)
            .border(width = 0.8.dp, color = colors.borderSubtle)
            .testTag("timeline_ruler")
            .pointerInput(totalMs, fps) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    latestOnScrubStart()

                    fun seekAtX(xPx: Float) {
                        val xDp = (xPx / density.density).coerceAtLeast(0f)
                        val rawMs = TimelineGeometry.msAtContentXDp(xDp, safeZoom)
                        latestOnSeek(
                            snapTargetMs(
                                rawMs = rawMs,
                                fps = fps,
                                totalMs = totalMs,
                                enabled = latestSnap,
                                candidates = latestCandidates,
                                thresholdFrames = latestThreshold
                            )
                        )
                    }

                    try {
                        seekAtX(down.position.x)
                        drag(down.id) { change ->
                            change.consume()
                            seekAtX(change.position.x)
                            // Keep scrubbing past the viewport edge: nudge the
                            // timeline so the finger can keep walking the ruler.
                            TimelineAutoScroll.atContentPx(
                                contentPx = change.position.x,
                                density = density.density,
                                scrollState = scrollState
                            )
                        }
                    } finally {
                        latestOnScrubEnd()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val pxPerMs = TimelineGeometry.dpPerMs(safeZoom) * density.density

            // Brass bottom edge
            drawLine(
                color = colors.border.copy(alpha = 0.4f),
                start = Offset(0f, h),
                end = Offset(size.width, h),
                strokeWidth = 1.2f
            )

            // 200 ms graduations, 500 ms half marks, 1 s major marks.
            var timeMs = 0L
            while (timeMs <= totalMs + 4_000L) {
                val x = timeMs * pxPerMs
                val isSecond = timeMs % 1_000L == 0L
                val isHalfSecond = timeMs % 500L == 0L
                val tickHeight = when {
                    isSecond -> h * 0.55f
                    isHalfSecond -> h * 0.38f
                    else -> h * 0.22f
                }
                val tickColor = when {
                    isSecond -> colors.border
                    isHalfSecond -> colors.textSecondary.copy(alpha = 0.6f)
                    else -> colors.borderSubtle.copy(alpha = 0.7f)
                }
                drawLine(
                    color = tickColor,
                    start = Offset(x, 0f),
                    end = Offset(x, tickHeight),
                    strokeWidth = if (isSecond) 1.5f else 0.9f
                )
                timeMs += 200L
            }
        }

        // Stamped second markings
        var labelMs = 0L
        while (labelMs <= totalMs + 4_000L) {
            val sec = labelMs / 1_000L
            Text(
                text = String.format("%02d:%02d", sec / 60, sec % 60),
                color = colors.textPrimary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(
                    x = (labelMs * TimelineGeometry.dpPerMs(safeZoom)).dp + 2.dp,
                    y = 13.dp
                )
            )
            labelMs += labelStepMs
        }
    }
}
