package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SnapCandidate
import com.example.engine.TimelineGeometry
import com.example.model.TimelineMarker

/**
 * CapCut-exact time ruler matching the reference screenshot:
 * Clean ticks with stamped second text (00:00 · 00:01 · 00:02) and subtle center dots.
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
    beats: List<Long> = emptyList(),
    markers: List<TimelineMarker> = emptyList(),
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            .background(Color(0xFF101014))
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

            // 100ms small ticks, 500ms dots, 1s marks
            var timeMs = 0L
            while (timeMs <= totalMs + 8_000L) {
                val x = timeMs * pxPerMs
                val isSecond = timeMs % 1_000L == 0L
                val isHalfSecond = timeMs % 500L == 0L

                if (isSecond) {
                    // Small top tick for second
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, h * 0.25f),
                        strokeWidth = 1.2f
                    )
                } else if (isHalfSecond) {
                    // Center dot between seconds: "·"
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = 1.4f * density.density,
                        center = Offset(x, h * 0.45f)
                    )
                } else if (timeMs % 100L == 0L) {
                    // Subtle tick
                    drawLine(
                        color = Color.White.copy(alpha = 0.18f),
                        start = Offset(x, 0f),
                        end = Offset(x, h * 0.14f),
                        strokeWidth = 0.8f
                    )
                }

                timeMs += 100L
            }

            // Detected beats
            beats.forEach { beat ->
                val x = beat * pxPerMs
                drawLine(
                    color = Color(0xFF00F2FE).copy(alpha = 0.75f),
                    start = Offset(x, h * 0.10f),
                    end = Offset(x, h),
                    strokeWidth = 1.6f * density.density
                )
            }

            // Timeline markers
            markers.forEach { marker ->
                val x = marker.atMs * pxPerMs
                drawLine(
                    color = Color(marker.colorArgb),
                    start = Offset(x, 0f),
                    end = Offset(x, h * 0.85f),
                    strokeWidth = 2f * density.density
                )
            }
        }

        // Stamped second markings: 00:00, 00:01, 00:02...
        var labelMs = 0L
        while (labelMs <= totalMs + 8_000L) {
            val sec = labelMs / 1_000L
            Text(
                text = String.format("%02d:%02d", sec / 60, sec % 60),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.offset(
                    x = (labelMs * TimelineGeometry.dpPerMs(safeZoom)).dp + 1.dp,
                    y = 8.dp
                )
            )
            labelMs += labelStepMs
        }
    }
}
