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
import java.util.Locale

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
                    // Position target crosshair at exact touch point
                    val rx = (reticlePos.x * canvasSize.width) - 16.dp.toPx()
                    val ry = (reticlePos.y * canvasSize.height) - 16.dp.toPx()
                    IntOffset(rx.toInt(), ry.toInt())
                }
                .size(32.dp)
        ) {
            // Precision target at finger point
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(pickedColorHex))
                    .border(1.5.dp, Color.White, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(32.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(1.5.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // Elevated Magnifier Loupe: Floats ~64dp above the finger so user can clearly see the color!
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val lx = (reticlePos.x * canvasSize.width) - 34.dp.toPx()
                    // Shift above touch point, or below if near top boundary
                    val yShift = if (reticlePos.y * canvasSize.height < 80.dp.toPx()) 40.dp.toPx() else (-68.dp.toPx())
                    val ly = (reticlePos.y * canvasSize.height) + yShift
                    IntOffset(lx.toInt(), ly.toInt())
                }
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.9f))
                .border(2.5.dp, Color(pickedColorHex), CircleShape)
        ) {
            // Color Swatch Core
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(pickedColorHex))
                    .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            )

            // Hex Code Badge at center
            val hexText = String.format(Locale.US, "#%06X", (pickedColorHex and 0xFFFFFFL))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-3).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = hexText,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
