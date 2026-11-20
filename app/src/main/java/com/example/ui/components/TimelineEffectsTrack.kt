package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Effects MAY overlap in time - that is a feature (two effects can share the
 * same seconds and both render). To keep every chip visible and tappable the
 * track packs overlapping effects into separate lanes automatically: a chip
 * goes into the first lane whose previous effect already ended, otherwise a
 * new lane opens below. Removing effects re-flows the lanes, so nothing ever
 * turns into an unreadable stack.
 *
 * Drag a chip horizontally to move it in time (manual overlap included);
 * dropping it onto another effect simply packs it into the next lane.
 */
@Composable
fun TimelineEffectsTrack(
    effects: List<TimelineEffect>,
    msToDp: Float,
    onEffectClick: (TimelineEffect) -> Unit = {},
    onMoveEffect: (id: String, startTimelineMs: Long) -> Unit = { _, _ -> }
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
                        onMoveEffect = onMoveEffect
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectChip(
    effect: TimelineEffect,
    msToDp: Float,
    onEffectClick: (TimelineEffect) -> Unit,
    onMoveEffect: (id: String, startTimelineMs: Long) -> Unit
) {
    val colors = PencilTheme.colors
    val density = LocalDensity.current
    var dragStartMs by remember(effect.id) { mutableFloatStateOf(effect.startTimelineMs.toFloat()) }

    val startOffsetDp = (effect.startTimelineMs * msToDp).dp
    val widthDp = (effect.durationMs * msToDp).dp.coerceAtLeast(44.dp)

    Box(
        modifier = Modifier
            .offset(x = startOffsetDp)
            .width(widthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accentGreen.copy(alpha = 0.25f))
            .border(1.2.dp, colors.accentGreen, RoundedCornerShape(6.dp))
            .clickable { onEffectClick(effect) }
            // Horizontal drag moves the effect in time. Overlap is legal: on
            // release the lane packer re-flows around the new window.
            .pointerInput(effect.id, msToDp) {
                detectDragGestures(
                    onDragStart = { dragStartMs = effect.startTimelineMs.toFloat() },
                    onDragEnd = {
                        onMoveEffect(effect.id, dragStartMs.toLong().coerceAtLeast(0L))
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val deltaMs = with(density) { dragAmount.x.toDp().value / msToDp.coerceAtLeast(0.0001f) }
                    dragStartMs = (dragStartMs + deltaMs).coerceAtLeast(0f)
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
            overflow = TextOverflow.Ellipsis
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
