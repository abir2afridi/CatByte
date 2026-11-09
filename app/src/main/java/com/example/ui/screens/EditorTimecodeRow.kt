package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * CapCut Timecode display matching the reference screenshot:
 * "00:01 / 00:16" with active time in crisp white and total duration in muted gray.
 */
@Composable
fun EditorTimecodeText(
    currentPositionMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("editor_timecode_text"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTimecodeSeconds(currentPositionMs),
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
        Text(
            text = " / " + formatTimecodeSeconds(totalDurationMs),
            color = Color(0xFF8E8E93),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
internal fun EditorTimecodeRow(
    currentPositionMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF101014))
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTimecodeText(
            currentPositionMs = currentPositionMs,
            totalDurationMs = totalDurationMs
        )
    }
}

internal fun formatTimecodeSeconds(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSeconds = safe / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
