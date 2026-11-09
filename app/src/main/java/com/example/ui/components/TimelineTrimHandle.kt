package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.engine.ClipTrimHandle

/** Sky-blue tint a trim handle shows while the user is holding it (trim live). */
internal val TrimHandlePressedColor = Color(0xFF4DC3FF)

/**
 * CapCut-style inline edge handle: a bold white bar hugging the clip edge with
 * a wide invisible hit area, so grabbing "the white border" always trims.
 *
 * Own file on purpose — this is the tuned CapCut-parity trim gesture:
 *  - the drag is consumed by this child, so the timeline never scrolls under
 *    the finger while a handle is held: a held border means trim, not pan;
 *  - while pressed the bar tints sky blue so the user can see the trim is live;
 *  - dragging the END edge right restores hidden footage up to the source end,
 *    dragging it left shortens; the START edge mirrors it, bounded by the
 *    source start (the engine maths live in `ClipTrimHandle`);
 *  - callbacks are read through [rememberUpdatedState]: a drag can outlive the
 *    recomposition that created it (a split rebuilds the clip list), and a
 *    stale clip id would silently begin the trim on a clip that no longer
 *    exists.
 */
@Composable
internal fun TimelineTrimHandle(
    edge: ClipTrimHandle.Edge,
    color: Color,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit = {},
    onDragDelta: (Float) -> Unit = {},
    onCommit: (Float) -> Unit
) {
    var accumulated by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDragDelta by rememberUpdatedState(onDragDelta)
    val latestOnCommit by rememberUpdatedState(onCommit)

    val isStart = edge == ClipTrimHandle.Edge.START
    val barShape = RoundedCornerShape(
        topStart = if (isStart) 8.dp else 0.dp,
        bottomStart = if (isStart) 8.dp else 0.dp,
        topEnd = if (isStart) 0.dp else 8.dp,
        bottomEnd = if (isStart) 0.dp else 8.dp
    )
    val barColor = if (isPressed) TrimHandlePressedColor else color

    Box(
        modifier = modifier
            .fillMaxHeight()
            // Generous touch target: touching anywhere near the bold border is
            // a trim grab, not a timeline pan.
            .width(28.dp)
            .pointerInput(edge) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        isPressed = false
                    }
                }
            }
            .pointerInput(edge) {
                detectDragGestures(
                    onDragStart = {
                        accumulated = 0f
                        latestOnDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulated += dragAmount.x
                        latestOnDragDelta(accumulated)
                    },
                    onDragEnd = {
                        latestOnCommit(accumulated)
                        accumulated = 0f
                    },
                    onDragCancel = {
                        accumulated = 0f
                    }
                )
            },
        contentAlignment = if (isStart) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        // Visible white bar covering the clip edge (CapCut reference look)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(11.dp)
                .background(barColor, barShape)
        ) {
            // Dark notch so the bar reads as a draggable grip
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(3.dp)
                    .height(20.dp)
                    .background(Color(0xFF171A21))
            )
        }
    }
}
