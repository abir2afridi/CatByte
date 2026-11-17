package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.TimelineMarker

/**
 * Ruler pinned to the top of the track scroll. The leading/trailing
 * half-viewport spacers live INSIDE the scroll content.
 */
@Composable
internal fun TimelineRulerStrip(
    totalMs: Long,
    zoom: Float,
    beats: List<Long>,
    markers: List<TimelineMarker>,
    scrollState: ScrollState,
    leadingPadDp: Dp,
    trailingPadDp: Dp,
    timelineWidthDp: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .horizontalScroll(scrollState)
    ) {
        Row {
            Spacer(modifier = Modifier.width(leadingPadDp))
            Box(modifier = Modifier.width(timelineWidthDp.dp)) {
                TimelineRuler(
                    totalMs = totalMs,
                    zoom = zoom,
                    beats = beats,
                    markers = markers
                )
            }
            Spacer(modifier = Modifier.width(trailingPadDp))
        }
    }
}
