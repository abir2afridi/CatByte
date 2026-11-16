package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.engine.SnapCandidate
import com.example.engine.SnapEngine
import com.example.engine.TimelineGeometry
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Gestures for the scrolling timeline viewport:
 *
 * - pinch to change zoom;
 * - any press reports user interaction;
 * - a tap on the ruler seeks (snapped);
 * - a tap on empty timeline space clears the clip selection.
 *
 * The ruler tap deliberately does not consume the event so a drag still
 * reaches `horizontalScroll` and slides the ruler under the fixed needle.
 */
@Composable
internal fun Modifier.timelineViewportGestures(
    totalMs: Long,
    fps: Int,
    zoom: Float,
    viewportWidthPx: Int,
    needleInsetPx: Float,
    msToDp: Float,
    scrollState: ScrollState,
    magneticSnap: Boolean,
    snapCandidates: List<SnapCandidate>,
    snapThresholdFrames: Long,
    onUserInteraction: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onClearSelection: () -> Unit
): Modifier {
    val latestInteraction by rememberUpdatedState(onUserInteraction)
    val latestClearSelection by rememberUpdatedState(onClearSelection)
    val latestOnZoom by rememberUpdatedState(onZoomChange)
    val latestZoom by rememberUpdatedState(zoom)
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestSnap by rememberUpdatedState(magneticSnap)
    val latestSnapCandidates by rememberUpdatedState(snapCandidates)
    val latestSnapThreshold by rememberUpdatedState(snapThresholdFrames)

    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                var baselineSpan = 0f
                var baselineZoom = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size >= 2) {
                        latestInteraction()
                        val span = abs(pressed[0].position.x - pressed[1].position.x)
                        if (baselineSpan <= 0f) {
                            baselineSpan = span.coerceAtLeast(1f)
                            baselineZoom = latestZoom
                        } else if (span > 0f) {
                            val next = (baselineZoom * (span / baselineSpan))
                                .coerceIn(0.5f, 3f)
                            if (abs(next - latestZoom) > 0.001f) latestOnZoom(next)
                        }
                        event.changes.forEach { it.consume() }
                    }
                    if (event.changes.all { !it.pressed }) break
                }
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press) {
                        latestInteraction()
                    }
                }
            }
        }
        .pointerInput(totalMs, fps, zoom, viewportWidthPx, needleInsetPx) {
            // Tap on the ruler -> seek. Deliberately non-consuming:
            // a drag must still reach horizontalScroll so the ruler
            // slides under the fixed needle. The ruler itself has
            // no pointer input for exactly this reason.
            val touchSlop = viewConfiguration.touchSlop
            val rulerHeightPx = 28.dp.toPx()
            val pxPerMsPx = msToDp * density
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.position.y > rulerHeightPx) return@awaitEachGesture
                var moved = false
                var multiTouch = false
                var released = false
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } > 1) multiTouch = true
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.changedToUpIgnoreConsumed()) {
                        released = true
                        break
                    }
                    if (!change.pressed) break
                    if (abs(change.position.x - down.position.x) > touchSlop ||
                        abs(change.position.y - down.position.y) > touchSlop
                    ) {
                        moved = true
                    }
                }
                if (released && !moved && !multiTouch && pxPerMsPx > 0f) {
                    val contentX = scrollState.value + down.position.x
                    val padPx = needleInsetPx
                    val rawMs = ((contentX - padPx) / pxPerMsPx)
                        .roundToLong()
                        .coerceAtLeast(0L)
                    latestOnSeek(
                        snapTargetMs(
                            rawMs = rawMs,
                            fps = fps,
                            totalMs = totalMs,
                            enabled = latestSnap,
                            candidates = latestSnapCandidates,
                            thresholdFrames = latestSnapThreshold
                        )
                    )
                }
            }
        }
        .pointerInput(Unit) {
            // A tap that reaches this parent is a tap on EMPTY
            // timeline space: clips have their own clickable, which
            // consumes the down before it gets here. Deselect the
            // clip so the white border / trim handles go away.
            detectTapGestures(onTap = { latestClearSelection() })
        }
}
