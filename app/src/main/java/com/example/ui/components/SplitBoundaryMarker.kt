package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * The split marker CapCut shows on the timeline: a small white pill with a
 * vertical line sitting exactly on the boundary where a clip was split, so the
 * user can see where the cut happened (tapping it opens the Transitions sheet).
 *
 * Own file on purpose — the visibility rule is the tuned CapCut-parity
 * behaviour: the marker must HIDE the moment either neighbouring clip becomes
 * selected (single or multi-select). The selected clip's bold trim frame and
 * its [TimelineTrimHandle] bars occupy the same boundary spot, and this
 * marker's touch target sits exactly on the handles — if it stayed visible it
 * would eat the handle touches and trimming at the split would break. The
 * marker comes back as soon as the clip is deselected.
 */
@Composable
internal fun SplitBoundaryMarker(
    leftClipId: String,
    rightClipId: String,
    selectedClipId: String?,
    selectedClipIds: Set<String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val leftSelected = leftClipId == selectedClipId || leftClipId in selectedClipIds
    val rightSelected = rightClipId == selectedClipId || rightClipId in selectedClipIds
    if (leftSelected || rightSelected) return

    Box(
        modifier = modifier
            .size(width = 22.dp, height = 30.dp)
            .zIndex(15f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(0.5.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(14.dp)
                .background(Color.Black)
        )
    }
}
