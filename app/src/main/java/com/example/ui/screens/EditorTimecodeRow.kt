package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme
import java.util.Locale

/**
 * Timecode strip above the timeline, matching the reference walkthrough:
 * current playhead time on the left, total project duration on the right.
 */
@Composable
internal fun EditorTimecodeRow(
    currentPositionMs: Long,
    totalDurationMs: Long
) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(colors.background)
            .padding(horizontal = 12.dp)
            .testTag("editor_timecode_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTimecode(currentPositionMs),
            color = colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = formatTimecode(totalDurationMs),
            color = colors.textTertiary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

internal fun formatTimecode(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalTenths = safe / 100L
    val minutes = totalTenths / 600L
    val seconds = (totalTenths / 10L) % 60L
    val tenths = totalTenths % 10L
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
}
