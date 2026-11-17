package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Dedicated editable beat grid overlay for the timeline ruler.
 */
@Composable
fun TimelineBeatGrid(
    durationMs: Long,
    scrollOffset: Float,
    zoom: Float,
    beats: List<Long>,
    onBeatAdded: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pixelPerMs = 0.1f * zoom
    val gridColor = Color(0xFFFFD700).copy(alpha = 0.7f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val width = size.width
        for (beatMs in beats) {
            val x = (beatMs * pixelPerMs) - scrollOffset
            if (x >= 0 && x <= width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f
                )
            }
        }
    }
}
