package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PencilTheme
import com.example.ui.theme.SharpenedPencilTip

/**
 * Fixed centre playhead matching CapCut.
 *
 * The needle NEVER moves and has no drag gesture: the caller lays this
 * full-size box over the whole timeline panel with top-centre alignment, so
 * the line is always exactly at the horizontal centre of the panel — right
 * under the play button above it. The timeline scrolls underneath it and the
 * time under the needle is the current position (see `engine/CenterPlayhead.kt`
 * for the scroll<->time maths).
 *
 * [headHeight] is the empty strip reserved above the ruler: the pencil (or
 * gold cap) lives there so it never covers the ruler's time labels, and the
 * needle starts exactly where that strip ends.
 *
 * [active] only changes the line thickness while the timeline is being
 * scrolled; it never moves the needle.
 */
@Composable
fun TimelinePlayhead(
    active: Boolean = false,
    headHeight: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors

    Box(
        modifier = modifier.testTag("timeline_playhead"),
        contentAlignment = Alignment.TopCenter
    ) {
        if (!colors.isPencilSketch) {
            // CapCut white needle line — spans ruler and every track.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(if (active) 2.5.dp else 2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .testTag("timeline_playhead_needle")
            )

            // Gold top pointer cap, sitting in the reserved head strip.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 12.dp, height = 11.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFFFFB300))
            )
        } else {
            // Soft halo on the ruler where the needle crosses, so the exact
            // position stands out without hiding the time labels.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = headHeight)
                    .width(6.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            // Solid graphite line: the line IS the current position, it must
            // not read as a faint grid line. It starts under the pencil tip.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = headHeight)
                    .width(if (active) 3.dp else 2.5.dp)
                    .fillMaxHeight()
                    .background(colors.playhead)
                    .testTag("timeline_playhead_needle")
            )

            // Pencil tip drawn LAST, in the reserved strip ABOVE the ruler,
            // so the ruler's time labels stay fully readable.
            SharpenedPencilTip(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}
