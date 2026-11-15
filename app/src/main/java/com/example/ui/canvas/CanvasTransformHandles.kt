package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.model.MediaClip
import com.example.ui.theme.PencilTheme

/**
 * The CapCut-style selection bounding box and transform handles drawn over
 * the clip that is being transformed.
 */
@Composable
internal fun CanvasTransformHandles(
    selectedClip: MediaClip,
    canvasSize: androidx.compose.ui.unit.IntSize
) {
    val colors = PencilTheme.colors
    val clipScale = selectedClip.scale
    val clipRotation = selectedClip.rotation
    val clipPosX = selectedClip.posX
    val clipPosY = selectedClip.posY

    Box(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val boxWidthDp = with(density) { (canvasSize.width * 0.80f * clipScale).toDp() }
        val boxHeightDp = with(density) { (canvasSize.height * 0.80f * clipScale).toDp() }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val cx = clipPosX * canvasSize.width
                    val cy = clipPosY * canvasSize.height
                    val halfW = (canvasSize.width * 0.40f * clipScale)
                    val halfH = (canvasSize.height * 0.40f * clipScale)
                    IntOffset((cx - halfW).toInt(), (cy - halfH).toInt())
                }
                .size(boxWidthDp.coerceAtLeast(40.dp), boxHeightDp.coerceAtLeast(40.dp))
                .rotate(clipRotation)
                .border(1.5.dp, colors.accentCyan, RoundedCornerShape(4.dp))
        ) {
            listOf(
                Alignment.TopStart to IntOffset(-6, -6),
                Alignment.TopEnd to IntOffset(6, -6),
                Alignment.BottomStart to IntOffset(-6, 6),
                Alignment.BottomEnd to IntOffset(6, 6)
            ).forEach { (align, offs) ->
                Box(
                    modifier = Modifier
                        .align(align)
                        .offset(offs.x.dp, offs.y.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, colors.accentCyan, CircleShape)
                )
            }

            // Top Rotation Handle
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.accentYellow)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⟳", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Black)
            }

            // Badge showing scale and rotation
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(0.8.dp, colors.accentCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${(clipScale * 100).toInt()}% • ${clipRotation.toInt()}°",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
