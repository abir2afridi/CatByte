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
 * CapCut timecode display in the timeline's left gutter, e.g.
 * "00:01.8 / 01:07.1": active time in crisp white, total duration in muted
 * gray, both with sub-second precision so scrubbing is easy to read.
 *
 * This is the single timecode in the editor - nothing time-related is painted
 * on top of the preview frame.
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
            text = formatTimecodeTenths(currentPositionMs),
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.3).sp,
            maxLines = 1
        )
        Text(
            text = " / " + formatTimecodeTenths(totalDurationMs),
            color = Color(0xFF8E8E93),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.3).sp,
            maxLines = 1
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

/**
 * Formats milliseconds as `MM:SS.s` using locale-independent ASCII digits and
 * a dot separator (Locale.US), so Bangla/Arabic locales never localise the
 * digits or swap the decimal separator.
 */
internal fun formatTimecodeTenths(ms: Long): String {
    // Work in integer tenths of a second so 59.96s never rounds to "00:60.0".
    val totalTenths = (ms.coerceAtLeast(0L) + 50L) / 100L
    val minutes = totalTenths / 600L
    val seconds = (totalTenths % 600L) / 10L
    val tenths = totalTenths % 10L
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
}
