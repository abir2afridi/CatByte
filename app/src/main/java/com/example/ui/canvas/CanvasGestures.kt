package com.example.ui.canvas

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/**
 * The pointer handling of the canvas frame.
 *
 * Three modes share the same frame:
 *  - eyedropper: drag or tap to sample a chroma-key colour;
 *  - transform: pinch / rotate / pan the selected clip;
 *  - plain: a tap toggles playback.
 *
 * The canvas size is passed as a [State] so a gesture always sees the latest
 * measured frame, even if the layout changed after the gesture started.
 */
internal fun Modifier.canvasGestures(
    isEyedropperActive: Boolean,
    isTransformInteractive: Boolean,
    selectedClipId: String?,
    canvasSize: State<IntSize>,
    onReticleMoved: (Offset) -> Unit,
    onSampleColorAt: ((normX: Float, normY: Float) -> Unit)?,
    onTransformClip: ((scaleMultiplier: Float, rotationDelta: Float, panDeltaX: Float, panDeltaY: Float) -> Unit)?,
    onTogglePlay: () -> Unit
): Modifier = this
    .pointerInput(isEyedropperActive) {
        if (isEyedropperActive) {
            detectDragGestures(
                onDragStart = { offset ->
                    val nx = (offset.x / canvasSize.value.width.toFloat()).coerceIn(0f, 1f)
                    val ny = (offset.y / canvasSize.value.height.toFloat()).coerceIn(0f, 1f)
                    onReticleMoved(Offset(nx, ny))
                    onSampleColorAt?.invoke(nx, ny)
                },
                onDrag = { change, _ ->
                    change.consume()
                    val nx = (change.position.x / canvasSize.value.width.toFloat()).coerceIn(0f, 1f)
                    val ny = (change.position.y / canvasSize.value.height.toFloat()).coerceIn(0f, 1f)
                    onReticleMoved(Offset(nx, ny))
                    onSampleColorAt?.invoke(nx, ny)
                }
            )
        }
    }
    .pointerInput(isEyedropperActive, isTransformInteractive, selectedClipId) {
        if (isEyedropperActive) {
            detectTapGestures { offset ->
                val nx = (offset.x / canvasSize.value.width.toFloat()).coerceIn(0f, 1f)
                val ny = (offset.y / canvasSize.value.height.toFloat()).coerceIn(0f, 1f)
                onReticleMoved(Offset(nx, ny))
                onSampleColorAt?.invoke(nx, ny)
            }
        } else if (isTransformInteractive && selectedClipId != null) {
            detectTransformGestures { _, pan, zoom, rotation ->
                val panDeltaX = pan.x / canvasSize.value.width.toFloat()
                val panDeltaY = pan.y / canvasSize.value.height.toFloat()
                onTransformClip?.invoke(zoom, rotation, panDeltaX, panDeltaY)
            }
        } else {
            detectTapGestures { onTogglePlay() }
        }
    }
