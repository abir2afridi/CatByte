package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TextOverlay
import com.example.ui.theme.PencilTheme

/** Text / subtitle track. */
@Composable
fun TimelineTextTrack(
    texts: List<TextOverlay>,
    selectedTextId: String?,
    msToDp: Float,
    onSelectText: (String) -> Unit
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        texts.forEach { item ->
            val isSelected = item.id == selectedTextId
            val startOffsetDp = (item.startTimelineMs * msToDp).dp
            val widthDp = (item.durationMs * msToDp).dp.coerceAtLeast(48.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentYellow.copy(alpha = if (isSelected) 0.5f else 0.25f))
                    .border(
                        width = if (isSelected) 2.dp else 1.2.dp,
                        color = colors.accentYellow,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectText(item.id) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text",
                        tint = colors.accentYellow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = item.text,
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
