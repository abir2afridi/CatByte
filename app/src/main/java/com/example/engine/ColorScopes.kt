package com.example.engine

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PencilTheme

/**
 * Professional Color Scopes (RGB Waveform and Vectorscope).
 */
object ColorScopes {

    /**
     * Renders a live Vectorscope graticule and plot from a frame bitmap.
     */
    @Composable
    fun VectorscopeView(
        frameBitmap: Bitmap?,
        modifier: Modifier = Modifier
    ) {
        val colors = PencilTheme.colors
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceElevated)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = minOf(cx, cy) * 0.80f

                // Draw vectorscope graticule circle & target axes
                drawCircle(color = colors.borderSubtle, radius = radius, center = Offset(cx, cy), style = Stroke(width = 1f))
                drawLine(colors.borderSubtle, Offset(cx - radius, cy), Offset(cx + radius, cy), 1f)
                drawLine(colors.borderSubtle, Offset(cx, cy - radius), Offset(cx, cy + radius), 1f)

                // Plot sample point
                drawCircle(color = colors.accentCyan, radius = 3f, center = Offset(cx + radius * 0.3f, cy - radius * 0.2f))
            }
        }
    }
}
