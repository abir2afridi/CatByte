package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.PencilTheme

/**
 * The CapCut-style eyedropper magnifier reticle.
 *
 * It follows the sampled point and shows the colour that was picked there.
 */
@Composable
internal fun CanvasEyedropperReticle(
    reticlePos: Offset,
    canvasSize: androidx.compose.ui.unit.IntSize,
    pickedColorHex: Long
) {
    val colors = PencilTheme.colors
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val rx = (reticlePos.x * canvasSize.width) - 26.dp.toPx()
                    val ry = (reticlePos.y * canvasSize.height) - 26.dp.toPx()
                    IntOffset(rx.toInt(), ry.toInt())
                }
                .size(52.dp)
        ) {
            // Outer ring with sampled color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(3.dp, Color(pickedColorHex), CircleShape)
            )
            // Inner circle border
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(1.5.dp, Color.White, CircleShape)
            )
            // Center dot with sampled color
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(pickedColorHex))
                    .border(1.5.dp, Color.White, CircleShape)
            )
            // Crosshairs
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(52.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.7f))
            )
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(1.5.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.7f))
            )
        }

        // Bottom instruction pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, colors.accentCyan, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🎯 Tap or drag reticle to pick chroma key color",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
