package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CapCut export selectors are horizontal tracks with a dot and label per stop.
 * Shared by the resolution, frame rate and bitrate selectors; each feature
 * owns its own options and header - this file only owns the track drawing.
 */
@Composable
internal fun ExportSelectorTrack(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val y = 6.dp.toPx()
                val inset = size.width / (labels.size * 2f)
                drawLine(
                    color = Color(0xFF2D303B),
                    start = Offset(inset, y),
                    end = Offset(size.width - inset, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(index) }
                    .testTag("export_selector_$label")
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color(0xFF2D303B))
                        .border(
                            width = 1.dp,
                            color = if (selected) Color.White else Color(0xFF3D404D),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFFA5A8B2),
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/** A "Label   hint" header row shared by the export feature rows. */
@Composable
internal fun ExportFeatureHeader(
    label: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            hint,
            color = Color(0xFFA5A8B2),
            fontSize = 10.sp
        )
    }
}
