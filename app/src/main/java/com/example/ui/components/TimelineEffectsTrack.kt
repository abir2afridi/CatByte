package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimelineEffect
import com.example.ui.theme.PencilTheme

/**
 * Visual effects track.
 *
 * [POSITION-LOCKED — user approved 2026-11-21] Overlapping effects are a
 * feature: multiple effects may share the same seconds and both render.
 * Do NOT add collision checks that push effects apart.
 *
 * Effects MAY overlap in time - that is a feature (two effects can share the
 * same seconds and both render). To keep every chip visible and tappable the
 * track packs overlapping effects into separate lanes automatically: a chip
 * goes into the first lane whose previous effect already ended, otherwise a
 * new lane opens below. Removing effects re-flows the lanes, so nothing ever
 * turns into an unreadable stack.
 *
 * Chips behave like video clips:
 *  - drag the middle to move the window in time (the chip follows the finger
 *    frame by frame, then commits once on release);
 *  - drag the left/right edge handle to trim the window (300 ms minimum);
 *  - dropping onto another effect's window is legal - both render and the
 *    lanes re-flow around the new windows.
 */
@Composable
fun TimelineEffectsTrack(
    effects: List<TimelineEffect>,
    msToDp: Float,
    onEffectClick: (TimelineEffect) -> Unit = {},
    onMoveEffect: (id: String, startTimelineMs: Long) -> Unit = { _, _ -> },
    onResizeEffect: (id: String, startTimelineMs: Long, durationMs: Long) -> Unit = { _, _, _ -> }
) {
    val colors = PencilTheme.colors
    val lanes = remember(effects) { packEffectLanes(effects) }
    val laneHeight = 24.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height((laneHeight * lanes.size + 6.dp).coerceAtLeast(30.dp))
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        lanes.forEach { lane ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(laneHeight)
            ) {
                lane.forEach { effect ->
                    EffectChip(
                        effect = effect,
                        msToDp = msToDp,
                        onEffectClick = onEffectClick,
                        onMoveEffect = onMoveEffect,
                        onResizeEffect = onResizeEffect
                    )
                }
            }
        }
    }
}

private enum class EffectDragMode { MOVE, TRIM_START, TRIM_END }

@Composable
private fun EffectChip(
    effect: TimelineEffect,
    msToDp: Float,
    onEffectClick: (TimelineEffect) -> Unit,
    onMoveEffect: (id: String, startTimelineMs: Long) -> Unit,
    onResizeEffect: (id: String, startTimelineMs: Long, durationMs: Long) -> Unit
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current

    // Live drag state: the chip follows the finger pixel-by-pixel via these
    // local values and commits ONCE to the view model on release - no jump.
    var dragMode by remember(effect.id) { mutableStateOf<EffectDragMode?>(null) }
    var dragDeltaMs by remember(effect.id) { mutableFloatStateOf(0f) }

    val liveStart = effect.startTimelineMs + when (dragMode) {
        EffectDragMode.MOVE, EffectDragMode.TRIM_START -> dragDeltaMs
        else -> 0f
    }
    val liveDuration = when (dragMode) {
        EffectDragMode.TRIM_START -> effect.durationMs - dragDeltaMs
        EffectDragMode.TRIM_END -> effect.durationMs + dragDeltaMs
        else -> effect.durationMs.toFloat()
    }.coerceAtLeast(300f)

    val startOffsetDp = (liveStart.coerceAtLeast(0f) * msToDp).dp
    val widthDp = (liveDuration * msToDp).dp.coerceAtLeast(44.dp)
    val edgeGrabDp = 10.dp

    Box(
        modifier = Modifier
            .offset(x = startOffsetDp)
            .width(widthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accentGreen.copy(alpha = 0.25f))
            .border(1.2.dp, colors.accentGreen, RoundedCornerShape(6.dp))
            // Tap opens the editor; a real drag is consumed by the gesture
            // detector above so the tap never fires after a move.
            .pointerInput(effect.id) {
                detectTapGestures {
                    onEffectClick(effect)
                }
            }
            .pointerInput(effect.id, msToDp) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        // Decide the gesture from where the finger landed:
                        // near an edge = trim, middle = move.
                        val widthPx = widthDp.toPx()
                        val x = startOffset.x
                        dragMode = when {
                            x <= edgeGrabDp.toPx() -> EffectDragMode.TRIM_START
                            x >= widthPx - edgeGrabDp.toPx() -> EffectDragMode.TRIM_END
                            else -> EffectDragMode.MOVE
                        }
                        dragDeltaMs = 0f
                    },
                    onDragEnd = {
                        val start = liveStart.toLong().coerceAtLeast(0L)
                        val duration = liveDuration.toLong().coerceIn(300L, 30_000L)
                        when (dragMode) {
                            EffectDragMode.MOVE -> onMoveEffect(effect.id, start)
                            EffectDragMode.TRIM_START, EffectDragMode.TRIM_END ->
                                onResizeEffect(effect.id, start, duration)
                            null -> Unit
                        }
                        dragMode = null
                        dragDeltaMs = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val deltaMs =
                        with(density) { dragAmount.x.toDp().value / msToDp.coerceAtLeast(0.0001f) }
                    when (dragMode) {
                        EffectDragMode.MOVE -> {
                            dragDeltaMs += deltaMs
                            // Never let the window start before 0.
                            if (effect.startTimelineMs + dragDeltaMs < 0f) {
                                dragDeltaMs = -effect.startTimelineMs.toFloat()
                            }
                        }
                        EffectDragMode.TRIM_START -> {
                            dragDeltaMs += deltaMs
                            // Keep at least 300 ms and never before 0.
                            val maxDelta = (effect.durationMs - 300f)
                                .coerceAtLeast(0f)
                            dragDeltaMs = dragDeltaMs.coerceIn(
                                -effect.startTimelineMs.toFloat(),
                                maxDelta
                            )
                        }
                        EffectDragMode.TRIM_END -> {
                            dragDeltaMs += deltaMs
                            dragDeltaMs = dragDeltaMs.coerceAtLeast(300f - effect.durationMs)
                        }
                        null -> Unit
                    }
                }
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "FX: ${effect.name}",
            color = colors.textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        // Trim affordances: slim cyan bars on both edges, like the clip trim
        // handles. They sit above the tap/click target.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.handle.copy(alpha = 0.8f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.handle.copy(alpha = 0.8f))
        )
    }
}

/**
 * Greedy interval packing: the FIRST lane whose last effect ends at or before
 * this effect starts. Overlapping effects therefore land in different lanes
 * (both stay visible), non-overlapping ones share a lane. Deterministic and
 * cheap - it re-runs whenever the effect list changes.
 */
internal fun packEffectLanes(effects: List<TimelineEffect>): List<List<TimelineEffect>> {
    val sorted = effects.sortedWith(compareBy({ it.startTimelineMs }, { it.id }))
    val laneEnds = mutableListOf<Long>()
    val lanes = mutableListOf<MutableList<TimelineEffect>>()
    sorted.forEach { effect ->
        val end = effect.startTimelineMs + effect.durationMs.coerceAtLeast(1L)
        val laneIndex = laneEnds.indexOfFirst { it <= effect.startTimelineMs }
        if (laneIndex >= 0) {
            laneEnds[laneIndex] = end
            lanes[laneIndex].add(effect)
        } else {
            laneEnds.add(end)
            lanes.add(mutableListOf(effect))
        }
    }
    return lanes
}
