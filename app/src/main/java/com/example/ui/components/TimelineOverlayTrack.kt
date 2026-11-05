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
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaClip
import com.example.ui.theme.PencilTheme

/** Picture-in-picture / overlay track. Clips are positioned by timeline start. */
@Composable
fun TimelineOverlayTrack(
    clips: List<MediaClip>,
    selectedClipId: String?,
    msToDp: Float,
    onSelectClip: (String) -> Unit
) {
    val colors = PencilTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(colors.timelineTrack)
            .padding(vertical = 2.dp)
    ) {
        clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId
            val startOffsetDp = (clip.startTimelineMs * msToDp).dp
            val widthDp = (clip.durationMs * msToDp).dp.coerceAtLeast(48.dp)

            Box(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accentCyan.copy(alpha = if (isSelected) 0.42f else 0.22f))
                    .border(
                        width = if (isSelected) 2.2.dp else 1.2.dp,
                        color = colors.accentCyan,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (clip.keyframes.isNotEmpty()) {
                    clip.keyframes.forEach { kf ->
                        val kfOffsetMs = kf.timestampMs.coerceIn(0L, clip.durationMs)
                        val kfXDp = (kfOffsetMs * msToDp).dp
                        Box(
                            modifier = Modifier
                                .offset(x = (kfXDp - 4.dp).coerceAtLeast(0.dp), y = 4.dp)
                                .size(8.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(Color(0xFFFF3B30), RoundedCornerShape(1.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(1.dp))
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Overlay clip",
                        tint = colors.accentCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = clip.title,
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
